package com.pettrip.mypage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.mypage.service.MyPageService;
import com.pettrip.mypage.service.MyPageSummary;
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
@WebMvcTest(MyPageController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class MyPageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MyPageService myPageService;

  @Test
  void 마이페이지_요약을_조회한다() throws Exception {
    when(myPageService.getSummary(any()))
        .thenReturn(new MyPageSummary("초롱", "chorong@example.com", 2L));

    mockMvc
        .perform(get("/users/me/mypage"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mypage-summary",
                responseFields(
                    fieldWithPath("nickname").description("닉네임"),
                    fieldWithPath("email").description("이메일"),
                    fieldWithPath("petCount").description("등록한 반려견 수"))));
  }
}
