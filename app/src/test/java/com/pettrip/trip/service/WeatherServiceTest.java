package com.pettrip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pettrip.trip.model.CourseWeatherRecord;
import com.pettrip.trip.model.StartCourse;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.repository.CourseWeatherRecordRepository;
import com.pettrip.trip.repository.TravelCourseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

  @Mock private TravelCourseRepository travelCourseRepository;
  @Mock private CourseWeatherRecordRepository weatherRecordRepository;

  @InjectMocks private WeatherService weatherService;

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");
  private static final UUID COURSE_ID = UUID.fromString("0198f3a0-5678-7000-8000-000000000002");

  private TravelCourse sampleCourse(UUID userId) {
    StartCourse start = new StartCourse("강남구", LocalDateTime.of(2026, 8, 1, 10, 0));
    return new TravelCourse(userId, start, LocalDate.of(2026, 8, 1));
  }

  private CourseWeatherRecord sampleRecord(UUID courseId) {
    return new CourseWeatherRecord(
        courseId, LocalDate.of(2026, 8, 1), (short) 28, (short) 65, "맑음", null);
  }

  @Test
  void 날씨를_저장한다() {
    TravelCourse course = sampleCourse(USER_ID);
    CourseWeatherRecord record = sampleRecord(COURSE_ID);
    when(travelCourseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
    when(weatherRecordRepository.save(any())).thenReturn(record);

    CourseWeatherRecord result =
        weatherService.saveWeather(
            USER_ID, COURSE_ID, LocalDate.of(2026, 8, 1), (short) 28, (short) 65, "맑음", null);

    assertThat(result.getCourseId()).isEqualTo(COURSE_ID);
    assertThat(result.getTemperature()).isEqualTo((short) 28);
    assertThat(result.getWeatherStatus()).isEqualTo("맑음");
  }

  @Test
  void 코스가_없으면_날씨_저장_시_예외가_발생한다() {
    when(travelCourseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                weatherService.saveWeather(
                    USER_ID,
                    COURSE_ID,
                    LocalDate.of(2026, 8, 1),
                    (short) 28,
                    (short) 65,
                    "맑음",
                    null))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void 코스_소유자가_아니면_날씨_저장_시_예외가_발생한다() {
    UUID otherId = UUID.fromString("0198f3a0-9999-7000-8000-000000000099");
    TravelCourse course = sampleCourse(otherId);
    when(travelCourseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

    assertThatThrownBy(
            () ->
                weatherService.saveWeather(
                    USER_ID,
                    COURSE_ID,
                    LocalDate.of(2026, 8, 1),
                    (short) 28,
                    (short) 65,
                    "맑음",
                    null))
        .isInstanceOf(CourseNotOwnerException.class);
  }

  @Test
  void 날씨_목록을_날짜_내림차순으로_조회한다() {
    TravelCourse course = sampleCourse(USER_ID);
    List<CourseWeatherRecord> records = List.of(sampleRecord(COURSE_ID));
    when(travelCourseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
    when(weatherRecordRepository.findByCourseIdOrderByWeatherDateDesc(COURSE_ID))
        .thenReturn(records);

    List<CourseWeatherRecord> result = weatherService.getWeather(USER_ID, COURSE_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getWeatherStatus()).isEqualTo("맑음");
  }

  @Test
  void 코스가_없으면_날씨_조회_시_예외가_발생한다() {
    when(travelCourseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> weatherService.getWeather(USER_ID, COURSE_ID))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void 코스_소유자가_아니면_날씨_조회_시_예외가_발생한다() {
    UUID otherId = UUID.fromString("0198f3a0-9999-7000-8000-000000000099");
    TravelCourse course = sampleCourse(otherId);
    when(travelCourseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> weatherService.getWeather(USER_ID, COURSE_ID))
        .isInstanceOf(CourseNotOwnerException.class);
  }
}
