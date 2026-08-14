package com.pettrip.pet.service;

import com.pettrip.pet.model.PetActivity;
import com.pettrip.pet.repository.PetActivityRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 반려동물 등록에 쓸 활동 유형 선택지를 제공한다.
 *
 * <p>{@code pet_activities} 테이블은 V1에 있었지만 값도, 엔티티도, 조회 API도 없었다. 그래서 {@code
 * pet_preferences_activities}를 채울 방법이 아예 없었다.
 *
 * <p>{@link BreedService}, {@link com.pettrip.user.service.PreferenceOptionService}와 같은 이유로 존재한다.
 * 코드값이 {@code gen_random_uuid()}로 생성되어 환경마다 다르므로 클라이언트가 하드코딩할 수 없다.
 */
@Service
public class PetActivityService {

  private final PetActivityRepository petActivityRepository;

  public PetActivityService(PetActivityRepository petActivityRepository) {
    this.petActivityRepository = petActivityRepository;
  }

  /** 이름 오름차순으로 돌려준다. 선택 목록에 그대로 뿌릴 수 있어야 한다. */
  public List<PetActivity> findAll() {
    return petActivityRepository.findAllByOrderByActivityNameAsc();
  }
}
