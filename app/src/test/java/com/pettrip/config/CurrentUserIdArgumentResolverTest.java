package com.pettrip.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pettrip.common.service.CurrentUserId;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentUserIdArgumentResolverTest {

  private final CurrentUserIdArgumentResolver resolver = new CurrentUserIdArgumentResolver();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void UUID_타입에_붙은_CurrentUserId_파라미터를_지원한다() throws Exception {
    assertThat(resolver.supportsParameter(parameterOf("annotated", 0))).isTrue();
  }

  @Test
  void 애노테이션이_없는_파라미터는_지원하지_않는다() throws Exception {
    assertThat(resolver.supportsParameter(parameterOf("notAnnotated", 0))).isFalse();
  }

  @Test
  void UUID가_아닌_타입에_붙으면_지원하지_않는다() throws Exception {
    assertThat(resolver.supportsParameter(parameterOf("wrongType", 0))).isFalse();
  }

  @Test
  void JWT의_sub_클레임을_UUID로_변환해_반환한다() throws Exception {
    UUID userId = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");
    authenticateWithSubject(userId.toString());

    Object resolved = resolver.resolveArgument(parameterOf("annotated", 0), null, null, null);

    assertThat(resolved).isEqualTo(userId);
  }

  @Test
  void 인증_정보가_없으면_예외를_던진다() {
    assertThatThrownBy(
            () -> resolver.resolveArgument(parameterOf("annotated", 0), null, null, null))
        .isInstanceOf(UnauthenticatedRequestException.class);
  }

  @Test
  void JWT_인증이_아니면_예외를_던진다() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("someone", "credentials"));

    assertThatThrownBy(
            () -> resolver.resolveArgument(parameterOf("annotated", 0), null, null, null))
        .isInstanceOf(UnauthenticatedRequestException.class);
  }

  @Test
  void sub_클레임이_UUID_형식이_아니면_예외를_던진다() {
    authenticateWithSubject("not-a-uuid");

    assertThatThrownBy(
            () -> resolver.resolveArgument(parameterOf("annotated", 0), null, null, null))
        .isInstanceOf(UnauthenticatedRequestException.class);
  }

  private void authenticateWithSubject(String subject) {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getSubject()).thenReturn(subject);
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }

  private MethodParameter parameterOf(String methodName, int index) throws Exception {
    Method method =
        switch (methodName) {
          case "annotated" -> Target.class.getDeclaredMethod("annotated", UUID.class);
          case "notAnnotated" -> Target.class.getDeclaredMethod("notAnnotated", UUID.class);
          case "wrongType" -> Target.class.getDeclaredMethod("wrongType", String.class);
          default -> throw new IllegalArgumentException(methodName);
        };
    return new MethodParameter(method, index);
  }

  @SuppressWarnings("unused")
  private static final class Target {
    void annotated(@CurrentUserId UUID userId) {}

    void notAnnotated(UUID userId) {}

    void wrongType(@CurrentUserId String userId) {}
  }
}
