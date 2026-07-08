package com.pettrip.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.common.config.JpaConfig;
import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaConfig.class)
class PlaceRepositoryTest {

  @Autowired private PlaceRepository placeRepository;
  @Autowired private PlacePetPolicyRepository petPolicyRepository;

  private Place savedPlace() {
    Place place =
        new Place(
            "kakao-test-001",
            null,
            "테스트 공원",
            null,
            "서울시 테스트구",
            new BigDecimal("37.5000"),
            new BigDecimal("127.0000"),
            null,
            null,
            (short) 3);
    return placeRepository.save(place);
  }

  @Test
  void 장소를_저장하고_외부ID로_조회한다() {
    Place saved = savedPlace();

    Optional<Place> found = placeRepository.findById("kakao-test-001");

    assertThat(found).isPresent();
    assertThat(found.get().getPlaceName()).isEqualTo("테스트 공원");
    assertThat(found.get().getRating()).isEqualTo((short) 3);
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  void 장소_펫_정책을_저장하고_조회한다() {
    Place place = savedPlace();
    PlacePetPolicy policy =
        new PlacePetPolicy(
            place, AllowedPetSize.SMALL, true, false, IndoorOutdoorType.INDOOR, false, null);
    petPolicyRepository.save(policy);

    Optional<PlacePetPolicy> found = petPolicyRepository.findById("kakao-test-001");

    assertThat(found).isPresent();
    assertThat(found.get().getAllowedPetSize()).isEqualTo(AllowedPetSize.SMALL);
    assertThat(found.get().getLeashRequired()).isTrue();
  }
}
