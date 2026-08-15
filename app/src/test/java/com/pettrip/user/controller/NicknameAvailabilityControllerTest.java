package com.pettrip.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.user.service.UserService;
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
@WebMvcTest(NicknameAvailabilityController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class NicknameAvailabilityControllerTest {

  private static final String USER_ID = "0198f3a0-1234-7000-8000-000000000001";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 사용_가능한_닉네임이면_available_true를_반환한다() throws Exception {
    when(userService.isNicknameAvailable("초롱이")).thenReturn(true);

    mockMvc
        .perform(
            get("/users/nickname/availability")
                .param("nickname", "초롱이")
                .with(jwt().jwt(j -> j.subject(USER_ID))))
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
        .perform(
            get("/users/nickname/availability")
                .param("nickname", "초롱이")
                .with(jwt().jwt(j -> j.subject(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(false));
  }

  @Test
  void 닉네임_파라미터가_없으면_400을_반환한다() throws Exception {
    mockMvc
        .perform(get("/users/nickname/availability").with(jwt().jwt(j -> j.subject(USER_ID))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 빈_닉네임이면_400을_반환한다() throws Exception {
    mockMvc
        .perform(
            get("/users/nickname/availability")
                .param("nickname", " ")
                .with(jwt().jwt(j -> j.subject(USER_ID))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 닉네임이_30자를_넘으면_400을_반환한다() throws Exception {
    mockMvc
        .perform(
            get("/users/nickname/availability")
                .param("nickname", "가".repeat(31))
                .with(jwt().jwt(j -> j.subject(USER_ID))))
        .andExpect(status().isBadRequest());
  }

  /**
   * addFilters=false 대신 실제 {@link SecurityConfig}를 올렸기 때문에 이 검증이 의미를 갖는다. 필터를 끄면 인증 설정이 잘못돼도 테스트가
   * 통과한다 (docs/failures/022 참고).
   */
  @Test
  void 토큰이_없어도_조회할_수_있다() throws Exception {
    when(userService.isNicknameAvailable("초롱이")).thenReturn(true);
    mockMvc
        .perform(get("/users/nickname/availability").param("nickname", "초롱이"))
        .andExpect(status().isOk());
  }
}
