package com.pettrip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.pettrip.place.model.Place;
import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceService;
import com.pettrip.trip.model.CoursePlace;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.repository.CoursePlaceRepository;
import com.pettrip.trip.repository.TravelCourseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  @Mock private PlaceService placeService;
  @Mock private PlaceRepository placeRepository;
  @Mock private TravelCourseRepository travelCourseRepository;
  @Mock private CoursePlaceRepository coursePlaceRepository;

  @InjectMocks private CourseService courseService;

  private Place samplePlace(String id, String name) {
    return new Place(
        id,
        null,
        name,
        null,
        "서울시",
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        null,
        null,
        null);
  }

  @Test
  void 주변_장소로_코스를_생성한다() {
    List<Place> places = List.of(samplePlace("p1", "장소A"), samplePlace("p2", "장소B"));
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(places);
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(coursePlaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TravelCourse result =
        courseService.createCourse(
            UUID.randomUUID(),
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            5000,
            LocalDate.now(),
            "강남구");

    assertThat(result).isNotNull();
    assertThat(result.getTravelDate()).isEqualTo(LocalDate.now());
  }

  @Test
  void 마지막_장소에_finalPlace_true가_설정된다() {
    List<Place> places =
        List.of(samplePlace("p1", "A"), samplePlace("p2", "B"), samplePlace("p3", "C"));
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(places);
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var saved = new java.util.ArrayList<CoursePlace>();
    when(coursePlaceRepository.save(any(CoursePlace.class)))
        .thenAnswer(
            inv -> {
              saved.add(inv.getArgument(0));
              return inv.getArgument(0);
            });

    courseService.createCourse(
        UUID.randomUUID(),
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        5000,
        LocalDate.now(),
        "강남구");

    assertThat(saved).hasSize(3);
    assertThat(saved.get(0).isFinalPlace()).isFalse();
    assertThat(saved.get(2).isFinalPlace()).isTrue();
  }

  @Test
  void 존재하지_않는_코스_조회시_예외가_발생한다() {
    UUID courseId = UUID.randomUUID();
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.getCourse(courseId))
        .isInstanceOf(CourseNotFoundException.class);
  }
}
