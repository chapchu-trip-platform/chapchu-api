package com.pettrip.pet.controller;

import com.pettrip.pet.service.PetActivityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 반려견 활동 유형 목록 조회.
 *
 * <p>유저별 데이터가 아니라 공용 코드값이므로 {@code /pets} 아래가 아니라 별도 경로에 둔다. {@code /pets}는 "내 반려동물"이라 인증이 필요한 반면
 * 이건 누구에게나 같은 값이다.
 *
 * <p>인증 없이 열어 둔다. 신규 유저는 온보딩 화면에서 반려동물과 활동을 함께 등록하는데, 그 시점에는 아직 access token이 없고 registration
 * token만 갖고 있다.
 */
@RestController
@RequestMapping("/activities")
public class PetActivityController {

  private final PetActivityService petActivityService;

  public PetActivityController(PetActivityService petActivityService) {
    this.petActivityService = petActivityService;
  }

  @GetMapping
  public List<PetActivityResponse> getActivities() {
    return petActivityService.findAll().stream().map(PetActivityResponse::from).toList();
  }
}
