package com.pettrip.trip.controller;

import com.pettrip.trip.model.CourseWeatherRecord;
import java.time.LocalDate;
import java.util.UUID;

public record WeatherRecordResponse(
    UUID id,
    UUID courseId,
    LocalDate weatherDate,
    Short temperature,
    Short humidity,
    String weatherStatus,
    String weatherCaution) {

  public static WeatherRecordResponse from(CourseWeatherRecord record) {
    return new WeatherRecordResponse(
        record.getId(),
        record.getCourseId(),
        record.getWeatherDate(),
        record.getTemperature(),
        record.getHumidity(),
        record.getWeatherStatus(),
        record.getWeatherCaution());
  }
}
