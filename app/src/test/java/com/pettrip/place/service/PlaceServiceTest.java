package com.pettrip.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.place.model.Place;
import com.pettrip.place.repository.PlacePetPolicyRepository;
import com.pettrip.place.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.List;
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
  @Mock private TourApiClient tourApiClient;

  @InjectMocks private PlaceService placeService;

  private Place samplePlace() {
    return new Place(
        "12345678",
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

  private TourApiClient.NearbyItem sampleItem(String contentId, String name) {
    return new TourApiClient.NearbyItem(
        contentId,
        "12",
        name,
        null,
        "서울시 영등포구",
        new BigDecimal("37.5263"),
        new BigDecimal("126.9342"),
        new BigDecimal("100"));
  }

  @Test
  void 존재하는_장소를_조회한다() {
    when(placeRepository.findById("12345678")).thenReturn(Optional.of(samplePlace()));

    Place result = placeService.getPlace("12345678");

    assertThat(result.getExternalPlaceId()).isEqualTo("12345678");
    assertThat(result.getPlaceName()).isEqualTo("한강공원");
  }

  @Test
  void 존재하지_않는_장소_조회시_예외가_발생한다() {
    when(placeRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> placeService.getPlace("unknown"))
        .isInstanceOf(PlaceNotFoundException.class);
  }

  @Test
  void TourAPI_결과를_DB에_동기화하고_반환한다() {
    TourApiClient.NearbyItem item = sampleItem("12345678", "한강공원");
    when(tourApiClient.fetchNearby(any(), any(), anyInt())).thenReturn(List.of(item));
    when(placeRepository.findById("12345678")).thenReturn(Optional.empty());
    when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(petPolicyRepository.existsById("12345678")).thenReturn(true);

    List<Place> result =
        placeService.searchNearby(new BigDecimal("37.5263"), new BigDecimal("126.9342"), 5000);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPlaceName()).isEqualTo("한강공원");
    verify(placeRepository).save(any(Place.class));
  }

  @Test
  void TourAPI_결과가_없으면_빈_목록을_반환한다() {
    when(tourApiClient.fetchNearby(any(), any(), anyInt())).thenReturn(List.of());

    List<Place> result =
        placeService.searchNearby(new BigDecimal("37.5263"), new BigDecimal("126.9342"), 5000);

    assertThat(result).isEmpty();
  }

  @Test
  void pet_policy가_이미_DB에_있으면_fetchPetDetail을_호출하지_않는다() {
    TourApiClient.NearbyItem item = sampleItem("12345678", "한강공원");
    when(tourApiClient.fetchNearby(any(), any(), anyInt())).thenReturn(List.of(item));
    when(placeRepository.findById("12345678")).thenReturn(Optional.of(samplePlace()));
    when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(petPolicyRepository.existsById("12345678")).thenReturn(true);

    placeService.searchNearby(new BigDecimal("37.5263"), new BigDecimal("126.9342"), 5000);

    verify(tourApiClient, never()).fetchPetDetail("12345678");
  }

  @Test
  void TourAPI_호출이_실패해도_빈_목록을_반환한다() {
    when(tourApiClient.fetchNearby(any(), any(), anyInt()))
        .thenThrow(new RuntimeException("TourAPI 연결 실패"));

    List<Place> result =
        placeService.searchNearby(new BigDecimal("37.5263"), new BigDecimal("126.9342"), 5000);

    assertThat(result).isEmpty();
  }

  @Test
  void 이미_캐시된_장소는_정보를_갱신한다() {
    Place existing = samplePlace();
    TourApiClient.NearbyItem item = sampleItem("12345678", "한강공원(업데이트)");
    when(tourApiClient.fetchNearby(any(), any(), anyInt())).thenReturn(List.of(item));
    when(placeRepository.findById("12345678")).thenReturn(Optional.of(existing));
    when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(petPolicyRepository.existsById("12345678")).thenReturn(true);

    List<Place> result =
        placeService.searchNearby(new BigDecimal("37.5263"), new BigDecimal("126.9342"), 5000);

    assertThat(result.get(0).getPlaceName()).isEqualTo("한강공원(업데이트)");
  }
}
