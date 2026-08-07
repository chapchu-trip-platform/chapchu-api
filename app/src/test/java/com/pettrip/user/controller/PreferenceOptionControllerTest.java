package com.pettrip.user.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.user.model.Region;
import com.pettrip.user.model.Theme;
import com.pettrip.user.model.TransportMethod;
import com.pettrip.user.service.PreferenceOptionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(PreferenceOptionController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PreferenceOptionControllerTest {

  private static final String USER_ID = "0198f3a0-1234-7000-8000-000000000001";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PreferenceOptionService preferenceOptionService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 선호_사항_선택지를_조회한다() throws Exception {
    Mockito.when(preferenceOptionService.findRegions())
        .thenReturn(List.of(new Region("서울"), new Region("부산")));
    Mockito.when(preferenceOptionService.findThemes()).thenReturn(List.of(new Theme("카페")));
    Mockito.when(preferenceOptionService.findTransportMethods())
        .thenReturn(List.of(new TransportMethod("자가용")));

    mockMvc
        .perform(get("/preferences/options").with(jwt().jwt(j -> j.subject(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.regions[0].name").value("서울"))
        .andExpect(jsonPath("$.regions[0].id").exists())
        .andExpect(jsonPath("$.themes[0].name").value("카페"))
        .andExpect(jsonPath("$.transportMethods[0].name").value("자가용"))
        .andDo(
            document(
                "preference-options",
                responseFields(
                    fieldWithPath("regions[].id").description("지역 ID. 선호 사항 등록 시 `regionIds`에 넣는다"),
                    fieldWithPath("regions[].name").description("지역 이름"),
                    fieldWithPath("themes[].id").description("테마 ID. `themeIds`에 넣는다"),
                    fieldWithPath("themes[].name").description("테마 이름"),
                    fieldWithPath("transportMethods[].id")
                        .description("이동수단 ID. `transportMethodIds`에 넣는다"),
                    fieldWithPath("transportMethods[].name").description("이동수단 이름"))));
  }

  @Test
  void 토큰이_없으면_401을_반환한다() throws Exception {
    mockMvc.perform(get("/preferences/options")).andExpect(status().isUnauthorized());
  }
}
