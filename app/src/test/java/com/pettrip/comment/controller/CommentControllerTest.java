package com.pettrip.comment.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.comment.service.CommentService;
import com.pettrip.config.SecurityConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class CommentControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CommentService commentService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private CommentResponse sampleResponse(UUID postId, String content) {
    return new CommentResponse(
        UUID.randomUUID(),
        postId,
        null,
        0,
        1,
        content,
        "밤톨이아빠",
        LocalDateTime.of(2024, 1, 15, 10, 30, 0));
  }

  @Test
  void 댓글_목록을_조회한다() throws Exception {
    UUID postId = UUID.randomUUID();
    when(commentService.listComments(postId))
        .thenReturn(List.of(sampleResponse(postId, "좋은 글이네요")));

    mockMvc
        .perform(
            get("/posts/{postId}/comments", postId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nickname").value("밤톨이아빠"))
        .andDo(
            document(
                "comment-list",
                pathParameters(parameterWithName("postId").description("게시글 ID")),
                responseFields(
                    fieldWithPath("[].id").description("댓글 ID"),
                    fieldWithPath("[].postId").description("게시글 ID"),
                    fieldWithPath("[].parentCommentId")
                        .description("부모 댓글 ID (최상위 댓글이면 null)")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("[].depth").description("댓글 깊이"),
                    fieldWithPath("[].commentOrder").description("같은 글 내 정렬 순서"),
                    fieldWithPath("[].content").description("댓글 내용"),
                    fieldWithPath("[].nickname").description("작성자 닉네임. 탈퇴한 사용자면 (탈퇴한 사용자)"),
                    fieldWithPath("[].createdAt").description("작성일시"))));
  }

  @Test
  void 댓글을_작성한다() throws Exception {
    UUID postId = UUID.randomUUID();
    when(commentService.createComment(eq(USER_ID), eq(postId), isNull(), eq("좋은 글이네요")))
        .thenReturn(sampleResponse(postId, "좋은 글이네요"));

    String body = objectMapper.writeValueAsString(new CommentCreateRequest(null, "좋은 글이네요"));

    mockMvc
        .perform(
            post("/posts/{postId}/comments", postId)
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "comment-create",
                pathParameters(parameterWithName("postId").description("게시글 ID")),
                requestFields(
                    fieldWithPath("parentCommentId")
                        .description("부모 댓글 ID (최상위 댓글이면 null)")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("content").description("댓글 내용")),
                responseFields(
                    fieldWithPath("id").description("댓글 ID"),
                    fieldWithPath("postId").description("게시글 ID"),
                    fieldWithPath("parentCommentId")
                        .description("부모 댓글 ID")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("depth").description("댓글 깊이"),
                    fieldWithPath("commentOrder").description("같은 글 내 정렬 순서"),
                    fieldWithPath("content").description("댓글 내용"),
                    fieldWithPath("nickname").description("작성자 닉네임"),
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 댓글을_수정한다() throws Exception {
    UUID commentId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    when(commentService.updateComment(USER_ID, commentId, "고쳐 썼어요"))
        .thenReturn(sampleResponse(postId, "고쳐 썼어요"));

    String body = objectMapper.writeValueAsString(new CommentUpdateRequest("고쳐 썼어요"));

    mockMvc
        .perform(
            patch("/comments/{commentId}", commentId)
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("고쳐 썼어요"))
        .andDo(
            document(
                "comment-update",
                pathParameters(parameterWithName("commentId").description("댓글 ID")),
                requestFields(fieldWithPath("content").description("바꿀 댓글 내용. 필수")),
                responseFields(
                    fieldWithPath("id").description("댓글 ID"),
                    fieldWithPath("postId").description("게시글 ID"),
                    fieldWithPath("parentCommentId")
                        .description("부모 댓글 ID")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("depth").description("댓글 깊이"),
                    fieldWithPath("commentOrder").description("같은 글 내 정렬 순서"),
                    fieldWithPath("content").description("댓글 내용"),
                    fieldWithPath("nickname").description("작성자 닉네임"),
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 댓글_수정_시_내용이_비면_400() throws Exception {
    String body = objectMapper.writeValueAsString(new CommentUpdateRequest(" "));

    mockMvc
        .perform(
            patch("/comments/{commentId}", UUID.randomUUID())
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 댓글을_삭제한다() throws Exception {
    UUID commentId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/comments/{commentId}", commentId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "comment-delete",
                pathParameters(parameterWithName("commentId").description("댓글 ID"))));

    verify(commentService).deleteComment(USER_ID, commentId);
  }
}
