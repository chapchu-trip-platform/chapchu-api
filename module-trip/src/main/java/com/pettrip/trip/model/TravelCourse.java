package com.pettrip.trip.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "travel_courses")
@AttributeOverride(name = "id", column = @Column(name = "course_id"))
public class TravelCourse extends BaseEntity {

  @Column(name = "user_id")
  private UUID userId;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "start_course_id", nullable = false)
  private StartCourse startCourse;

  @Column(name = "travel_date")
  private LocalDate travelDate;

  @Column(name = "is_completed")
  private boolean isCompleted = false;

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @OrderBy("visitOrder ASC")
  private List<CoursePlace> coursePlaces = new ArrayList<>();

  protected TravelCourse() {}

  public TravelCourse(UUID userId, StartCourse startCourse, LocalDate travelDate) {
    this.userId = userId;
    this.startCourse = startCourse;
    this.travelDate = travelDate;
  }

  public UUID getUserId() {
    return userId;
  }

  public StartCourse getStartCourse() {
    return startCourse;
  }

  public LocalDate getTravelDate() {
    return travelDate;
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  public List<CoursePlace> getCoursePlaces() {
    return coursePlaces;
  }
}
