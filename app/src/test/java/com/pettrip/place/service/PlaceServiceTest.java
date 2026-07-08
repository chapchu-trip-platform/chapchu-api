package com.pettrip.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.repository.PlacePetPolicyRepository;
import com.pettrip.place.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

  @Mock private PlaceRepository placeRepository;
  @Mock private PlacePetPolicyRepository petPolicyRepository;

  @InjectMocks private PlaceService placeService;

  private Place samplePlace() {
    return new Place(
        "kakao-12345",
        null,
        "한강공원",
        null,
        "서울시 영등포구",
        new BigDecimal("37.5263"),
        new BigDecimal("126.9342"),
        null,
        null,
        (short) 4);
  }

  @Test
  void 존재하는_장소를_조회한다() {
    Place place = samplePlace();
    when(placeRepository.findById("kakao-12345")).thenReturn(Optional.of(place));

    Place result = placeService.getPlace("kakao-12345");

    assertThat(result.getExternalPlaceId()).isEqualTo("kakao-12345");
    assertThat(result.getPlaceName()).isEqualTo("한강공원");
  }

  @Test
  void 존재하지_않는_장소_조회시_예외가_발생한다() {
    when(placeRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> placeService.getPlace("unknown"))
        .isInstanceOf(PlaceNotFoundException.class);
  }

  @Test
  void 새_장소를_등록한다() {
    when(placeRepository.findById("kakao-new")).thenReturn(Optional.empty());
    when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Place result =
        placeService.upsertPlace(
            "kakao-new",
            null,
            "뚝섬유원지",
            null,
            "서울시 광진구",
            new BigDecimal("37.5285"),
            new BigDecimal("127.0675"),
            null,
            null,
            (short) 5);

    assertThat(result.getExternalPlaceId()).isEqualTo("kakao-new");
    assertThat(result.getPlaceName()).isEqualTo("뚝섬유원지");
    verify(placeRepository).save(any(Place.class));
  }

  @Test
  void 이미_존재하는_장소는_정보를_갱신한다() {
    Place existing = samplePlace();
    when(placeRepository.findById("kakao-12345")).thenReturn(Optional.of(existing));
    when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Place result =
        placeService.upsertPlace(
            "kakao-12345", null, "한강공원(업데이트)", null, null, null, null, null, null, null);

    assertThat(result.getPlaceName()).isEqualTo("한강공원(업데이트)");
  }

  @Test
  void 장소_펫_정책을_신규_등록한다() {
    Place place = samplePlace();
    when(placeRepository.findById("kakao-12345")).thenReturn(Optional.of(place));
    when(petPolicyRepository.findById("kakao-12345")).thenReturn(Optional.empty());
    when(petPolicyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PlacePetPolicy result =
        placeService.upsertPetPolicy(
            "kakao-12345",
            AllowedPetSize.ALL,
            true,
            false,
            IndoorOutdoorType.OUTDOOR,
            true,
            "목줄 필수");

    assertThat(result.getAllowedPetSize()).isEqualTo(AllowedPetSize.ALL);
    assertThat(result.getIndoorOutdoorType()).isEqualTo(IndoorOutdoorType.OUTDOOR);
    verify(petPolicyRepository).save(any(PlacePetPolicy.class));
  }

  @Test
  void 존재하지_않는_장소에_정책_등록시_예외가_발생한다() {
    when(placeRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                placeService.upsertPetPolicy(
                    "unknown",
                    AllowedPetSize.ALL,
                    true,
                    false,
                    IndoorOutdoorType.OUTDOOR,
                    true,
                    null))
        .isInstanceOf(PlaceNotFoundException.class);
  }
}
