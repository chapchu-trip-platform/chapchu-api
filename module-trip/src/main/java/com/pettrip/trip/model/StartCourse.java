package com.pettrip.trip.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "start_course")
@AttributeOverride(name = "id", column = @Column(name = "start_course_id"))
public class StartCourse extends BaseEntity {

  @Column(name = "start_course_location", length = 255)
  private String startCourseLocation;

  @Column(name = "start_course_time")
  private LocalDateTime startCourseTime;

  protected StartCourse() {}

  public StartCourse(String startCourseLocation, LocalDateTime startCourseTime) {
    this.startCourseLocation = startCourseLocation;
    this.startCourseTime = startCourseTime;
  }

  public String getStartCourseLocation() {
    return startCourseLocation;
  }

  public LocalDateTime getStartCourseTime() {
    return startCourseTime;
  }
}
