package com.pettrip.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@WebMvcTest(PostReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PostReportControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PostService postService;

  @Test
  void 게시글을_신고한다() throws Exception {
    UUID postId = UUID.randomUUID();
    String body = objectMapper.writeValueAsString(new PostReportRequest("SPAM", "광고성 글입니다"));

    mockMvc
        .perform(
            post("/posts/{postId}/reports", postId).contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "post-report-create",
                pathParameters(parameterWithName("postId").description("게시글 ID")),
                requestFields(
                    fieldWithPath("reportReason").description("신고 사유"),
                    fieldWithPath("reportDetail").description("신고 상세 내용 (선택)"))));

    verify(postService).report(any(), eq(postId), eq("SPAM"), eq("광고성 글입니다"));
  }
}
