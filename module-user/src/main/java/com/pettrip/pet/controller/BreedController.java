package com.pettrip.pet.controller;

import com.pettrip.pet.service.BreedService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 견종 목록 조회.
 *
 * <p>유저별 데이터가 아니라 공용 코드값이므로 {@code /users/me} 아래가 아니라 별도 경로에 둔다.
 *
 * <p>인증 없이 열어 둔다. 신규 유저는 온보딩 화면에서 반려동물을 등록하는데, 그 시점에는 아직 access token이 없고 registration token만 갖고
 * 있다. 견종은 유저와 무관한 고정 코드값이라 공개해도 드러나는 정보가 없다.
 */
@RestController
@RequestMapping("/breeds")
public class BreedController {

  private final BreedService breedService;

  public BreedController(BreedService breedService) {
    this.breedService = breedService;
  }

  @GetMapping
  public List<BreedResponse> getBreeds() {
    return breedService.findAll().stream().map(BreedResponse::from).toList();
  }
}
