package com.pettrip.mypage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.mypage.service.MyPostService;
import com.pettrip.post.model.Post;
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
@WebMvcTest(MyPostController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class MyPostControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MyPostService myPostService;

  @Test
  void 작성한_게시글_목록을_조회한다() throws Exception {
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(myPostService.listMyPosts(any())).thenReturn(List.of(post));

    mockMvc
        .perform(get("/users/me/posts"))
        .andExpect(status().isOk())
        .andDo(document("my-post-list"));
  }
}
