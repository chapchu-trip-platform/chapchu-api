package com.pettrip.pet.service;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.repository.BreedRepository;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * 반려동물 등록에 쓸 견종 선택지를 제공한다.
 *
 * <p>{@code POST /pets}는 견종을 <b>UUID로</b> 받는데, 클라이언트가 그 UUID를 얻을 방법이 없었다. 견종은 {@code V2}/{@code V7}
 * 마이그레이션에서 {@code gen_random_uuid()}로 만들어져 환경마다 값이 달라 하드코딩할 수도 없다. 이 조회가 없으면 반려동물 등록 자체를 쓸 수 없다.
 *
 * <p>{@link com.pettrip.user.service.PreferenceOptionService}와 같은 이유로 존재한다.
 */
@Service
public class BreedService {

  private final BreedRepository breedRepository;

  public BreedService(BreedRepository breedRepository) {
    this.breedRepository = breedRepository;
  }

  /** 한국어 언어 순으로 정렬해 돌려준다. 선택 목록에 그대로 뿌릴 수 있어야 한다. */
  public List<Breed> findAll() {
    List<Breed> breeds = new ArrayList<>(breedRepository.findAll());
    Collator collator = Collator.getInstance(Locale.KOREAN);
    breeds.sort((a, b) -> collator.compare(a.getBreedName(), b.getBreedName()));
    return breeds;
  }
}
