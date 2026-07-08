package com.pettrip.comment.controller;

import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.comment.model.Comment;
import com.pettrip.comment.service.CommentService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class CommentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CommentService commentService;

  @Test
  void 댓글을_작성한다() throws Exception {
    UUID postId = UUID.randomUUID();
    Comment comment = new Comment(postId, UUID.randomUUID(), null, 0, 1, "좋은 글이네요");
    when(commentService.createComment(any(), eq(postId), isNull(), eq("좋은 글이네요")))
        .thenReturn(comment);

    String body = objectMapper.writeValueAsString(new CommentCreateRequest(null, "좋은 글이네요"));

    mockMvc
        .perform(
            post("/posts/{postId}/comments", postId).contentType("application/json").content(body))
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
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 댓글을_삭제한다() throws Exception {
    UUID commentId = UUID.randomUUID();

    mockMvc
        .perform(delete("/comments/{commentId}", commentId))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "comment-delete",
                pathParameters(parameterWithName("commentId").description("댓글 ID"))));

    verify(commentService).deleteComment(any(), eq(commentId));
  }
}
