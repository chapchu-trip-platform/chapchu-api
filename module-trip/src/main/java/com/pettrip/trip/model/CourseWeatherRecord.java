package com.pettrip.trip.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "course_weather_records")
@AttributeOverride(name = "id", column = @Column(name = "weather_id"))
public class CourseWeatherRecord extends BaseEntity {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "weather_date")
  private LocalDate weatherDate;

  @Column(name = "temperature")
  private Short temperature;

  @Column(name = "humidity")
  private Short humidity;

  @Column(name = "weather_status", length = 10)
  private String weatherStatus;

  @Column(name = "weather_caution", length = 30)
  private String weatherCaution;

  protected CourseWeatherRecord() {}

  public CourseWeatherRecord(
      UUID courseId,
      LocalDate weatherDate,
      Short temperature,
      Short humidity,
      String weatherStatus,
      String weatherCaution) {
    this.courseId = courseId;
    this.weatherDate = weatherDate;
    this.temperature = temperature;
    this.humidity = humidity;
    this.weatherStatus = weatherStatus;
    this.weatherCaution = weatherCaution;
  }

  public UUID getCourseId() {
    return courseId;
  }

  public LocalDate getWeatherDate() {
    return weatherDate;
  }

  public Short getTemperature() {
    return temperature;
  }

  public Short getHumidity() {
    return humidity;
  }

  public String getWeatherStatus() {
    return weatherStatus;
  }

  public String getWeatherCaution() {
    return weatherCaution;
  }
}
