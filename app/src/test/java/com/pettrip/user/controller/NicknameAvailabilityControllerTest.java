package com.pettrip.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.user.service.UserService;
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
@WebMvcTest(NicknameAvailabilityController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class NicknameAvailabilityControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @Test
  void 사용_가능한_닉네임이면_available_true를_반환한다() throws Exception {
    when(userService.isNicknameAvailable("초롱이")).thenReturn(true);

    mockMvc
        .perform(get("/users/nickname/availability").param("nickname", "초롱이"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("초롱이"))
        .andExpect(jsonPath("$.available").value(true))
        .andDo(
            document(
                "nickname-availability",
                queryParameters(parameterWithName("nickname").description("중복 확인할 닉네임")),
                responseFields(
                    fieldWithPath("nickname").description("확인한 닉네임"),
                    fieldWithPath("available").description("사용 가능 여부 (true=사용 가능)"))));
  }

  @Test
  void 이미_사용_중인_닉네임이면_available_false를_반환한다() throws Exception {
    when(userService.isNicknameAvailable("초롱이")).thenReturn(false);

    mockMvc
        .perform(get("/users/nickname/availability").param("nickname", "초롱이"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(false));
  }

  @Test
  void 닉네임_파라미터가_없으면_400을_반환한다() throws Exception {
    mockMvc.perform(get("/users/nickname/availability")).andExpect(status().isBadRequest());
  }

  @Test
  void 빈_닉네임이면_400을_반환한다() throws Exception {
    mockMvc
        .perform(get("/users/nickname/availability").param("nickname", " "))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 닉네임이_30자를_넘으면_400을_반환한다() throws Exception {
    mockMvc
        .perform(get("/users/nickname/availability").param("nickname", "가".repeat(31)))
        .andExpect(status().isBadRequest());
  }
}
