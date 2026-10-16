package com.pettrip.trip.service;

import com.pettrip.trip.model.CourseWeatherRecord;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.repository.CourseWeatherRecordRepository;
import com.pettrip.trip.repository.TravelCourseRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeatherService {

  private final TravelCourseRepository travelCourseRepository;
  private final CourseWeatherRecordRepository weatherRecordRepository;

  public WeatherService(
      TravelCourseRepository travelCourseRepository,
      CourseWeatherRecordRepository weatherRecordRepository) {
    this.travelCourseRepository = travelCourseRepository;
    this.weatherRecordRepository = weatherRecordRepository;
  }

  @Transactional
  public CourseWeatherRecord saveWeather(
      UUID userId,
      UUID courseId,
      LocalDate weatherDate,
      Short temperature,
      Short humidity,
      String weatherStatus,
      String weatherCaution) {
    TravelCourse course =
        travelCourseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);
    if (!userId.equals(course.getUserId())) {
      throw new CourseNotOwnerException();
    }
    CourseWeatherRecord record =
        new CourseWeatherRecord(
            courseId, weatherDate, temperature, humidity, weatherStatus, weatherCaution);
    return weatherRecordRepository.save(record);
  }

  @Transactional(readOnly = true)
  public List<CourseWeatherRecord> getWeather(UUID userId, UUID courseId) {
    TravelCourse course =
        travelCourseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);
    if (!userId.equals(course.getUserId())) {
      throw new CourseNotOwnerException();
    }
    return weatherRecordRepository.findByCourseIdOrderByWeatherDateDesc(courseId);
  }
}
