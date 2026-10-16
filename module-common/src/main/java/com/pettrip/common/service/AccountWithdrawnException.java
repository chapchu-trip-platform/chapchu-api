package com.pettrip.common.service;

/**
 * 탈퇴한 계정의 토큰으로 인증이 필요한 요청이 들어왔을 때.
 *
 * <p>docs/decisions/049 참고. 토큰 자체는 서명도 만료도 멀쩡하지만 계정이 더 이상 유효하지 않다. FE가 "토큰이 죽었다"와 같은 처리(저장한 토큰을 버리고
 * 로그인 화면으로)를 하면 되므로 401로 내려보낸다.
 */
public class AccountWithdrawnException extends UnauthorizedException {

  public AccountWithdrawnException() {
    super("사용할 수 없는 계정입니다.");
  }
}
