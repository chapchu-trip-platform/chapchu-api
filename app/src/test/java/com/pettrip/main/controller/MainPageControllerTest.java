package com.pettrip.main.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.main.service.MainPageService;
import com.pettrip.main.service.MainPageSummary;
import java.util.List;
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
@WebMvcTest(MainPageController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class MainPageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MainPageService mainPageService;

  @Test
  void 메인_페이지를_조회한다() throws Exception {
    when(mainPageService.getMainPage(any())).thenReturn(new MainPageSummary("초롱", List.of("루이")));

    mockMvc
        .perform(get("/home"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "main-page",
                responseFields(
                    fieldWithPath("nickname").description("닉네임"),
                    fieldWithPath("petNames").description("등록한 반려견 이름 목록"))));
  }
}
