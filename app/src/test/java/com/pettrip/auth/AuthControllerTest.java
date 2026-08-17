package com.pettrip.auth;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
@TestPropertySource(
    properties = {
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.chapchu.site",
      "chapchu-api.auth.fe-redirect-url=http://localhost:3000/auth/oauth",
      "chapchu-api.auth.callback-url=http://localhost:8080/auth/callback",
      "cors.allowed-origins=http://localhost:3000"
    })
class AuthControllerTest {

  private static final String ALLOWED_ORIGIN = "http://localhost:3000";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 로그아웃하면_refresh_token_쿠키가_만료된다() throws Exception {
    mockMvc
        .perform(
            post("/auth/logout")
                .header("Origin", ALLOWED_ORIGIN)
                .cookie(new Cookie("refresh_token", "some-refresh-token")))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
        .andDo(document("auth-logout"));
  }

  @Test
  void refresh_token_쿠키가_없어도_로그아웃은_성공한다() throws Exception {
    mockMvc
        .perform(post("/auth/logout").header("Origin", ALLOWED_ORIGIN))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
  }

  @Test
  void 허용되지_않은_origin으로_로그아웃하면_403을_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/auth/logout")
                .header("Origin", "https://evil.com")
                .cookie(new Cookie("refresh_token", "some-refresh-token")))
        .andExpect(status().isForbidden());
  }

  @Test
  void 콜백에서_code가_빈_문자열이면_400을_반환한다() throws Exception {
    mockMvc
        .perform(
            get("/auth/callback")
                .param("code", "")
                .param("state", "some-state")
                .cookie(new Cookie("oauth2_state", "some-state")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 콜백에서_state_쿠키가_없으면_400을_반환한다() throws Exception {
    mockMvc
        .perform(get("/auth/callback").param("code", "valid-code").param("state", "some-state"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 콜백에서_state_파라미터가_쿠키와_불일치하면_400을_반환한다() throws Exception {
    mockMvc
        .perform(
            get("/auth/callback")
                .param("code", "valid-code")
                .param("state", "tampered-state")
                .cookie(new Cookie("oauth2_state", "original-state")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 콜백에서_registration_token이_있으면_FE_온보딩으로_리다이렉트한다() throws Exception {
    mockMvc
        .perform(get("/auth/callback").param("registration_token", "reg-token-abc"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            header()
                .string(
                    "Location",
                    "http://localhost:3000/auth/oauth?registration_token=reg-token-abc"));
  }

  @Test
  void refresh_token_쿠키_없이_refresh하면_401을_반환한다() throws Exception {
    mockMvc
        .perform(post("/auth/refresh").header("Origin", ALLOWED_ORIGIN))
        .andExpect(status().isUnauthorized())
        .andDo(document("auth-refresh-no-cookie"));
  }

  @Test
  void 허용되지_않은_origin으로_refresh하면_403을_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/auth/refresh")
                .header("Origin", "https://evil.com")
                .cookie(new Cookie("refresh_token", "some-token")))
        .andExpect(status().isForbidden());
  }
}
