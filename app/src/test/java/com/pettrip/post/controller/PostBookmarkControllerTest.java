package com.pettrip.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.post.service.PostService;
import com.pettrip.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
@WebMvcTest(PostBookmarkController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PostBookmarkControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PostService postService;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @BeforeEach
  void 계정은_ACTIVE_상태다() {
    // 리졸버가 @CurrentUserId를 만들 때 계정 상태를 확인한다(docs/decisions/049).
    when(userService.isActive(any())).thenReturn(true);
  }

  @Test
  void 게시글을_북마크한다() throws Exception {
    UUID postId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/posts/{postId}/bookmarks", postId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "post-bookmark-create",
                pathParameters(parameterWithName("postId").description("게시글 ID"))));

    verify(postService).bookmark(USER_ID, postId);
  }

  @Test
  void 게시글_북마크를_취소한다() throws Exception {
    UUID postId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/posts/{postId}/bookmarks", postId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "post-bookmark-delete",
                pathParameters(parameterWithName("postId").description("게시글 ID"))));

    verify(postService).cancelBookmark(USER_ID, postId);
  }
}
