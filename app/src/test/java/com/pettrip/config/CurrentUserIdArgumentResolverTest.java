package com.pettrip.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pettrip.common.service.AccountWithdrawnException;
import com.pettrip.common.service.CurrentUserId;
import com.pettrip.user.service.UserService;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentUserIdArgumentResolverTest {

  private final UserService userService = mock(UserService.class);
  private final CurrentUserIdArgumentResolver resolver =
      new CurrentUserIdArgumentResolver(userService);

  @BeforeEach
  void 계정은_ACTIVE_상태다() {
    when(userService.isActive(any())).thenReturn(true);
  }

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
  void Optional_UUID_타입에_붙은_CurrentUserId도_지원한다() throws Exception {
    assertThat(resolver.supportsParameter(parameterOf("annotatedOptional", 0))).isTrue();
  }

  @Test
  void Optional_파라미터는_인증이_없으면_empty를_돌려준다() throws Exception {
    Object resolved =
        resolver.resolveArgument(parameterOf("annotatedOptional", 0), null, null, null);

    assertThat(resolved).isEqualTo(java.util.Optional.empty());
  }

  @Test
  void Optional_파라미터는_인증이_있으면_유저_id를_담아_돌려준다() throws Exception {
    UUID userId = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");
    authenticateWithSubject(userId.toString());

    Object resolved =
        resolver.resolveArgument(parameterOf("annotatedOptional", 0), null, null, null);

    assertThat(resolved).isEqualTo(java.util.Optional.of(userId));
  }

  /** docs/decisions/049 참고: 탈퇴한 계정에는 유저 id를 내주지 않는다. 401로 매핑된다. */
  @Test
  void 탈퇴한_계정이면_예외를_던진다() throws Exception {
    UUID userId = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");
    authenticateWithSubject(userId.toString());
    when(userService.isActive(userId)).thenReturn(false);

    assertThatThrownBy(
            () -> resolver.resolveArgument(parameterOf("annotated", 0), null, null, null))
        .isInstanceOf(AccountWithdrawnException.class);
  }

  /** 공개 엔드포인트용 Optional 변형은 막지 않고 비로그인으로 떨어뜨린다(docs/failures/028). */
  @Test
  void Optional_파라미터는_탈퇴한_계정이면_empty를_돌려준다() throws Exception {
    UUID userId = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");
    authenticateWithSubject(userId.toString());
    when(userService.isActive(userId)).thenReturn(false);

    Object resolved =
        resolver.resolveArgument(parameterOf("annotatedOptional", 0), null, null, null);

    assertThat(resolved).isEqualTo(java.util.Optional.empty());
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
          case "annotatedOptional" ->
              Target.class.getDeclaredMethod("annotatedOptional", java.util.Optional.class);
          default -> throw new IllegalArgumentException(methodName);
        };
    return new MethodParameter(method, index);
  }

  @SuppressWarnings("unused")
  private static final class Target {
    void annotated(@CurrentUserId UUID userId) {}

    void notAnnotated(UUID userId) {}

    void wrongType(@CurrentUserId String userId) {}

    void annotatedOptional(@CurrentUserId java.util.Optional<UUID> userId) {}
  }
}
