package com.pettrip.user.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.user.model.Region;
import com.pettrip.user.model.Theme;
import com.pettrip.user.model.TransportMethod;
import com.pettrip.user.model.User;
import com.pettrip.user.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
@WebMvcTest(UserPreferenceController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class UserPreferenceControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private User userWithPreferences() {
    User user = new User("test@example.com", "google-1");
    user.replacePreferredRegions(Set.of(new Region("서울")));
    user.replacePreferredThemes(Set.of(new Theme("카페")));
    user.replacePreferredTransportMethods(Set.of(new TransportMethod("자가용")));
    return user;
  }

  @Test
  void 선호_사항을_조회한다() throws Exception {
    when(userService.getPreferences(any())).thenReturn(userWithPreferences());

    mockMvc
        .perform(get("/users/me/preferences").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "user-preferences-get",
                responseFields(
                    fieldWithPath("regions[].id").description("지역 ID"),
                    fieldWithPath("regions[].name").description("지역 이름"),
                    fieldWithPath("themes[].id").description("테마 ID"),
                    fieldWithPath("themes[].name").description("테마 이름"),
                    fieldWithPath("transportMethods[].id").description("이동 수단 ID"),
                    fieldWithPath("transportMethods[].name").description("이동 수단 이름"))));
  }

  @Test
  void 선호_사항을_등록한다() throws Exception {
    UUID regionId = UUID.randomUUID();
    UUID themeId = UUID.randomUUID();
    UUID transportMethodId = UUID.randomUUID();
    when(userService.updatePreferences(
            any(), eq(List.of(regionId)), eq(List.of(themeId)), eq(List.of(transportMethodId))))
        .thenReturn(userWithPreferences());

    String body =
        objectMapper.writeValueAsString(
            new PreferenceRequest(List.of(regionId), List.of(themeId), List.of(transportMethodId)));

    mockMvc
        .perform(
            post("/users/me/preferences")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "user-preferences-register",
                requestFields(
                    fieldWithPath("regionIds").description("선호 지역 ID 목록"),
                    fieldWithPath("themeIds").description("선호 테마 ID 목록"),
                    fieldWithPath("transportMethodIds").description("선호 이동 수단 ID 목록")),
                responseFields(
                    fieldWithPath("regions[].id").description("지역 ID"),
                    fieldWithPath("regions[].name").description("지역 이름"),
                    fieldWithPath("themes[].id").description("테마 ID"),
                    fieldWithPath("themes[].name").description("테마 이름"),
                    fieldWithPath("transportMethods[].id").description("이동 수단 ID"),
                    fieldWithPath("transportMethods[].name").description("이동 수단 이름"))));
  }

  @Test
  void 선호_사항을_수정한다() throws Exception {
    UUID regionId = UUID.randomUUID();
    when(userService.updatePreferences(any(), eq(List.of(regionId)), eq(null), eq(null)))
        .thenReturn(userWithPreferences());

    String body =
        objectMapper.writeValueAsString(new PreferenceRequest(List.of(regionId), null, null));

    mockMvc
        .perform(
            patch("/users/me/preferences")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "user-preferences-update",
                requestFields(
                    fieldWithPath("regionIds").description("변경할 선호 지역 ID 목록 (선택)"),
                    fieldWithPath("themeIds").description("변경할 선호 테마 ID 목록 (선택)"),
                    fieldWithPath("transportMethodIds").description("변경할 선호 이동 수단 ID 목록 (선택)")),
                responseFields(
                    fieldWithPath("regions[].id").description("지역 ID"),
                    fieldWithPath("regions[].name").description("지역 이름"),
                    fieldWithPath("themes[].id").description("테마 ID"),
                    fieldWithPath("themes[].name").description("테마 이름"),
                    fieldWithPath("transportMethods[].id").description("이동 수단 ID"),
                    fieldWithPath("transportMethods[].name").description("이동 수단 이름"))));
  }
}
