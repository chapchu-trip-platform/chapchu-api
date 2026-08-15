package com.pettrip.signup.controller;

import com.pettrip.signup.service.SignupService;
import com.pettrip.signup.service.SignupService.SignupResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 온보딩 회원가입.
 *
 * <p>인증이 필요 없다. 이 시점에 사용자가 가진 것은 {@code registration_token}뿐이고 access token은 아직 없다. 토큰 검증이 곧 인증 역할을
 * 한다.
 */
@RestController
@RequestMapping("/auth/signup")
public class SignupController {

  private final SignupService signupService;

  public SignupController(SignupService signupService) {
    this.signupService = signupService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SignupResponse signUp(@RequestBody @Valid SignupRequest request) {
    SignupResult result = signupService.signUp(request);
    return SignupResponse.of(result.user(), result.pets());
  }
}
