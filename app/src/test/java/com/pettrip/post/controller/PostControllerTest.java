package com.pettrip.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.post.model.Post;
import com.pettrip.post.service.PostService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PostControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PostService postService;

  @Test
  void 게시글_목록을_조회한다() throws Exception {
    Post post =
        new Post(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "첫 여행",
            "즐거웠어요");
    when(postService.listPosts()).thenReturn(List.of(post));

    mockMvc.perform(get("/posts")).andExpect(status().isOk()).andDo(document("post-list"));
  }

  @Test
  void 게시글_상세를_조회한다() throws Exception {
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "첫 여행",
            "즐거웠어요");
    when(postService.getPost(postId)).thenReturn(post);

    mockMvc
        .perform(get("/posts/{postId}", postId))
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
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 게시글을_작성한다() throws Exception {
    UUID petId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Post post = new Post(UUID.randomUUID(), petId, photoId, courseId, "첫 여행", "즐거웠어요");
    when(postService.createPost(
            any(), eq(petId), eq(photoId), eq(courseId), eq("첫 여행"), eq("즐거웠어요")))
        .thenReturn(post);

    String body =
        objectMapper.writeValueAsString(
            new PostCreateRequest(petId, photoId, courseId, "첫 여행", "즐거웠어요"));

    mockMvc
        .perform(post("/posts").contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "post-create",
                requestFields(
                    fieldWithPath("petId").description("동행한 반려견 ID"),
                    fieldWithPath("photoId").description("대표 사진 ID"),
                    fieldWithPath("courseId").description("여행 코스 ID"),
                    fieldWithPath("title").description("제목"),
                    fieldWithPath("content").description("내용")),
                responseFields(
                    fieldWithPath("id").description("게시글 ID"),
                    fieldWithPath("petId").description("동행한 반려견 ID"),
                    fieldWithPath("photoId").description("대표 사진 ID"),
                    fieldWithPath("courseId").description("여행 코스 ID"),
                    fieldWithPath("title").description("제목"),
                    fieldWithPath("content").description("내용"),
                    fieldWithPath("viewCount").description("조회수"),
                    fieldWithPath("recommendationCount").description("추천 수"),
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 게시글을_수정한다() throws Exception {
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "수정된 제목",
            "수정된 내용");
    when(postService.updatePost(any(), eq(postId), eq("수정된 제목"), eq("수정된 내용"))).thenReturn(post);

    String body = objectMapper.writeValueAsString(new PostUpdateRequest("수정된 제목", "수정된 내용"));

    mockMvc
        .perform(patch("/posts/{postId}", postId).contentType("application/json").content(body))
        .andExpect(status().isOk())
        .andDo(
            document(
                "post-update",
                pathParameters(parameterWithName("postId").description("게시글 ID")),
                requestFields(
                    fieldWithPath("title").description("제목 (선택)"),
                    fieldWithPath("content").description("내용 (선택)")),
                responseFields(
                    fieldWithPath("id").description("게시글 ID"),
                    fieldWithPath("petId").description("동행한 반려견 ID"),
                    fieldWithPath("photoId").description("대표 사진 ID"),
                    fieldWithPath("courseId").description("여행 코스 ID"),
                    fieldWithPath("title").description("제목"),
                    fieldWithPath("content").description("내용"),
                    fieldWithPath("viewCount").description("조회수"),
                    fieldWithPath("recommendationCount").description("추천 수"),
                    fieldWithPath("createdAt").description("작성일시"))));
  }

  @Test
  void 게시글을_삭제한다() throws Exception {
    UUID postId = UUID.randomUUID();

    mockMvc
        .perform(delete("/posts/{postId}", postId))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "post-delete", pathParameters(parameterWithName("postId").description("게시글 ID"))));
  }
}
