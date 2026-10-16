package com.pettrip.common.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 요청의 유저 UUID를 컨트롤러 파라미터로 주입받는다.
 *
 * <p>docs/decisions/026 참고: 실제 값은 JWT의 {@code sub} 클레임(= {@code users.user_id})에서 추출된다(decision
 * 016). 추출은 app 모듈의 {@code CurrentUserIdArgumentResolver}가 담당하므로, 이 애노테이션을 쓰는 도메인 모듈은 Spring
 * Security에 의존하지 않는다.
 *
 * <p>{@code UUID} 또는 {@code Optional<UUID>} 파라미터에 쓸 수 있다. 공개 엔드포인트(비로그인 접근 허용)에서는 {@code
 * Optional<UUID>}를 써야 한다. {@code UUID}로 받으면 토큰이 없을 때 예외가 나 401로 떨어진다.
 *
 * <pre>{@code
 * @GetMapping
 * public List<PetResponse> listPets(@CurrentUserId UUID userId) { ... }
 * }</pre>
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {}
