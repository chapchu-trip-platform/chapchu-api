package com.pettrip.auth;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
      "chapchu-api.auth.callback-url=http://localhost:8080/auth/callback"
    })
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 로그아웃하면_refresh_token_쿠키가_만료된다() throws Exception {
    mockMvc
        .perform(post("/auth/logout").cookie(new Cookie("refresh_token", "some-refresh-token")))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge("refresh_token", 0))
        .andDo(document("auth-logout"));
  }
}
