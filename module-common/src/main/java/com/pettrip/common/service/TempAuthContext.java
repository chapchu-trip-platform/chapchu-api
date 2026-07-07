package com.pettrip.common.service;

import java.util.UUID;

/**
 * docs/decisions/013 참고: 실제 OAuth2 Resource Server 붙기 전까지 모든 도메인이 공유하는 임시 고정 유저 ID. auth 도메인 구현 시 각
 * Controller의 사용처를 @AuthenticationPrincipal 기반 추출로 교체하고 이 클래스는 제거한다.
 */
public final class TempAuthContext {

  public static final UUID TEMP_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private TempAuthContext() {}
}
