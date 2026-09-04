package com.pettrip.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.post.service.PostService;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PostControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PostService postService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private PostResponse samplePostResponse() {
    return new PostResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "첫 여행",
        "즐거웠어요",
        0,
        0,
        3,
        true,
        false,
        "멍멍이아빠",
        "https://example.com/photo.jpg",
        LocalDateTime.of(2024, 1, 15, 10, 30, 0));
  }

  @Test
  void 게시글_목록을_조회한다() throws Exception {
    PostListResponse listResponse = new PostListResponse(List.of(samplePostResponse()), null);
    when(postService.listPosts(any(), any(), any(), anyInt())).thenReturn(listResponse);

    mockMvc
        .perform(
            get("/posts")
                .param("sort", "latest")
                .param("size", "20")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "post-list",
                queryParameters(
                    parameterWithName("sort")
                        .optional()
                        .description("정렬 기준: latest(최신순, 기본값) / popular(추천순)"),
                    parameterWithName("cursor")
                        .optional()
                        .description(
                            "이전 페이지 마지막 항목의 커서. 형식: {createdAt}~{postId} (예: 2024-01-15T10:30:00~uuid). 첫 페이지 생략"),
                    parameterWithName("size").optional().description("페이지 크기 (기본값: 20)")),
                responseFields(
                    fieldWithPath("posts[]").description("게시글 목록"),
                    fieldWithPath("posts[].id").description("게시글 ID"),
                    fieldWithPath("posts[].petId").description("동행한 반려견 ID"),
                    fieldWithPath("posts[].photoId").description("대표 사진 ID"),
                    fieldWithPath("posts[].courseId").description("여행 코스 ID"),
                    fieldWithPath("posts[].title").description("제목"),
                    fieldWithPath("posts[].content").description("내용"),
                    fieldWithPath("posts[].viewCount").description("조회수"),
                    fieldWithPath("posts[].recommendationCount").description("추천 수"),
                    fieldWithPath("posts[].commentCount").description("댓글 수"),
                    fieldWithPath("posts[].recommended")
                        .description("요청한 사용자가 추천했는지. 추천 취소 버튼 노출 판단용"),
                    fieldWithPath("posts[].bookmarked").description("요청한 사용자가 북마크했는지"),
                    fieldWithPath("posts[].nickname").description("작성자 닉네임"),
                    fieldWithPath("posts[].photoUrl").description("대표 사진 URL (null 가능)").optional(),
                    fieldWithPath("posts[].createdAt").description("작성일시"),
                    fieldWithPath("nextCursor")
                        .description("다음 페이지 커서 (마지막 페이지면 null)")
                        .optional())));
  }

  @Test
  void 게시글_목록을_추천순으로_조회한다() throws Exception {
    PostListResponse listResponse = new PostListResponse(List.of(samplePostResponse()), null);
    when(postService.listPosts(any(), eq("popular"), any(), anyInt())).thenReturn(listResponse);

    mockMvc
        .perform(
            get("/posts")
                .param("sort", "popular")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk());
  }

  @Test
  void 게시글_상세를_조회한다() throws Exception {
    UUID postId = UUID.randomUUID();
    when(postService.getPost(USER_ID, postId)).thenReturn(samplePostResponse());

    mockMvc
        .perform(get("/posts/{postId}", postId).with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "post-detail",
                pathParameters(parameterWithName("postId").description("게시글 ID")),
                responseFields(
                    fieldWithPath("id").description("게시글 ID"),
                    fieldWithPath("petId").description("동행한 반려견 ID"),
                    fieldWithPath("photoId").description("대표 사진 ID"),
                    fieldWithPath("courseId").description("여행 코스 ID"),
                    fieldWithPath("title").description("제목"),
                    fieldWithPath("content").description("내용"),
                    fieldWithPath("viewCount").description("조회수"),
                    fieldWithPath("recommendationCount").description("추천 수"),
                    fieldWithPath("commentCount").description("댓글 수"),
                    fieldWithPath("recommended").description("요청한 사용자가 추천했는지. 추천 취소 버튼 노출 판단용"),
                    fieldWithPath("bookmarked").description("요청한 사용자가 북마크했는지"),
                    fieldWithPath("nickname").description("작성자 닉네임"),
                    fieldWithPath("photoUrl").description("대표 사진 URL (null 가능)").optional(),
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 게시글을_작성한다() throws Exception {
    UUID petId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    PostResponse response = samplePostResponse();
    when(postService.createPost(
            any(), eq(petId), eq(photoId), eq(courseId), eq("첫 여행"), eq("즐거웠어요")))
        .thenReturn(response);

    String body =
        objectMapper.writeValueAsString(
            new PostCreateRequest(petId, photoId, courseId, "첫 여행", "즐거웠어요"));

    mockMvc
        .perform(
            post("/posts")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "post-create",
                requestFields(
                    fieldWithPath("petId").description("동행한 반려견 ID"),
                    fieldWithPath("photoId").description("대표 사진 ID"),
                    fieldWithPath("courseId").description("여행 코스 ID"),
                    fieldWithPath("title").description("제목 (선택). 최대 100자").optional(),
                    fieldWithPath("content").description("내용 (선택)").optional()),
                responseFields(
                    fieldWithPath("id").description("게시글 ID"),
                    fieldWithPath("petId").description("동행한 반려견 ID"),
                    fieldWithPath("photoId").description("대표 사진 ID"),
                    fieldWithPath("courseId").description("여행 코스 ID"),
                    fieldWithPath("title").description("제목"),
                    fieldWithPath("content").description("내용"),
                    fieldWithPath("viewCount").description("조회수"),
                    fieldWithPath("recommendationCount").description("추천 수"),
                    fieldWithPath("commentCount").description("댓글 수"),
                    fieldWithPath("recommended").description("요청한 사용자가 추천했는지. 추천 취소 버튼 노출 판단용"),
                    fieldWithPath("bookmarked").description("요청한 사용자가 북마크했는지"),
                    fieldWithPath("nickname").description("작성자 닉네임"),
                    fieldWithPath("photoUrl").description("대표 사진 URL (null 가능)").optional(),
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 게시글_작성_시_제목이_100자를_넘으면_400() throws Exception {
    String body =
        objectMapper.writeValueAsString(
            new PostCreateRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "가".repeat(101), "즐거웠어요"));

    mockMvc
        .perform(
            post("/posts")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 게시글_작성_시_제목과_내용은_생략할_수_있다() throws Exception {
    UUID petId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    when(postService.createPost(any(), eq(petId), eq(photoId), eq(courseId), isNull(), isNull()))
        .thenReturn(samplePostResponse());

    String body =
        objectMapper.writeValueAsString(
            new PostCreateRequest(petId, photoId, courseId, null, null));

    mockMvc
        .perform(
            post("/posts")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated());
  }

  @Test
  void 게시글_수정_시_제목이_100자를_넘으면_400() throws Exception {
    String body = objectMapper.writeValueAsString(new PostUpdateRequest("가".repeat(101), null));

    mockMvc
        .perform(
            patch("/posts/{postId}", UUID.randomUUID())
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 게시글을_수정한다() throws Exception {
    UUID postId = UUID.randomUUID();
    PostResponse response = samplePostResponse();
    when(postService.updatePost(any(), eq(postId), eq("수정된 제목"), eq("수정된 내용")))
        .thenReturn(response);

    String body = objectMapper.writeValueAsString(new PostUpdateRequest("수정된 제목", "수정된 내용"));

    mockMvc
        .perform(
            patch("/posts/{postId}", postId)
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "post-update",
                pathParameters(parameterWithName("postId").description("게시글 ID")),
                requestFields(
                    fieldWithPath("title").description("제목 (선택). 최대 100자").optional(),
                    fieldWithPath("content").description("내용 (선택)").optional()),
                responseFields(
                    fieldWithPath("id").description("게시글 ID"),
                    fieldWithPath("petId").description("동행한 반려견 ID"),
                    fieldWithPath("photoId").description("대표 사진 ID"),
                    fieldWithPath("courseId").description("여행 코스 ID"),
                    fieldWithPath("title").description("제목"),
                    fieldWithPath("content").description("내용"),
                    fieldWithPath("viewCount").description("조회수"),
                    fieldWithPath("recommendationCount").description("추천 수"),
                    fieldWithPath("commentCount").description("댓글 수"),
                    fieldWithPath("recommended").description("요청한 사용자가 추천했는지. 추천 취소 버튼 노출 판단용"),
                    fieldWithPath("bookmarked").description("요청한 사용자가 북마크했는지"),
                    fieldWithPath("nickname").description("작성자 닉네임"),
                    fieldWithPath("photoUrl").description("대표 사진 URL (null 가능)").optional(),
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 게시글을_삭제한다() throws Exception {
    UUID postId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/posts/{postId}", postId).with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "post-delete", pathParameters(parameterWithName("postId").description("게시글 ID"))));
  }
}
