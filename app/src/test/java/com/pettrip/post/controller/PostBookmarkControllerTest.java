package com.pettrip.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.post.service.PostService;
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
@WebMvcTest(PostBookmarkController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PostBookmarkControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PostService postService;

  @Test
  void 게시글을_북마크한다() throws Exception {
    UUID postId = UUID.randomUUID();

    mockMvc
        .perform(post("/posts/{postId}/bookmarks", postId))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "post-bookmark-create",
                pathParameters(parameterWithName("postId").description("게시글 ID"))));

    verify(postService).bookmark(any(), eq(postId));
  }

  @Test
  void 게시글_북마크를_취소한다() throws Exception {
    UUID postId = UUID.randomUUID();

    mockMvc
        .perform(delete("/posts/{postId}/bookmarks", postId))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "post-bookmark-delete",
                pathParameters(parameterWithName("postId").description("게시글 ID"))));

    verify(postService).cancelBookmark(any(), eq(postId));
  }
}
