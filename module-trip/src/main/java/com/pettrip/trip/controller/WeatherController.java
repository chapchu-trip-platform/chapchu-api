package com.pettrip.trip.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.trip.service.WeatherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses/{courseId}/weather")
public class WeatherController {

  private final WeatherService weatherService;

  public WeatherController(WeatherService weatherService) {
    this.weatherService = weatherService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WeatherRecordResponse saveWeather(
      @CurrentUserId UUID userId,
      @PathVariable UUID courseId,
      @RequestBody @Valid WeatherRecordRequest request) {
    return WeatherRecordResponse.from(
        weatherService.saveWeather(
            userId,
            courseId,
            request.weatherDate(),
            request.temperature(),
            request.humidity(),
            request.weatherStatus(),
            request.weatherCaution()));
  }

  @GetMapping
  public List<WeatherRecordResponse> getWeather(
      @CurrentUserId UUID userId, @PathVariable UUID courseId) {
    return weatherService.getWeather(userId, courseId).stream()
        .map(WeatherRecordResponse::from)
        .toList();
  }
}
