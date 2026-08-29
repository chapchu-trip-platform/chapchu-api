package com.pettrip.trip.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_places")
@AttributeOverride(name = "id", column = @Column(name = "course_place_id"))
public class CoursePlace extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private TravelCourse course;

  @Column(name = "external_place_id", nullable = false, length = 255)
  private String externalPlaceId;

  @Column(name = "visit_order")
  private short visitOrder;

  @Column(name = "final_place")
  private boolean finalPlace = false;

  @Column(name = "is_visited")
  private boolean isVisited = false;

  @Column(name = "visited_at")
  private LocalDateTime visitedAt;

  protected CoursePlace() {}

  public CoursePlace(
      TravelCourse course, String externalPlaceId, short visitOrder, boolean finalPlace) {
    this.course = course;
    this.externalPlaceId = externalPlaceId;
    this.visitOrder = visitOrder;
    this.finalPlace = finalPlace;
  }

  public TravelCourse getCourse() {
    return course;
  }

  public String getExternalPlaceId() {
    return externalPlaceId;
  }

  public short getVisitOrder() {
    return visitOrder;
  }

  public boolean isFinalPlace() {
    return finalPlace;
  }

  public boolean isVisited() {
    return isVisited;
  }

  public LocalDateTime getVisitedAt() {
    return visitedAt;
  }

  public void markVisited() {
    this.isVisited = true;
    this.visitedAt = LocalDateTime.now();
  }
}
