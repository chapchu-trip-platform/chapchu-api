package com.pettrip.trip.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

  @Column(name = "start_location", length = 255)
  private String startLocation;

  @Column(name = "start_lat", precision = 10, scale = 7)
  private BigDecimal startLat;

  @Column(name = "start_lng", precision = 10, scale = 7)
  private BigDecimal startLng;

  @Column(name = "end_location", length = 255)
  private String endLocation;

  @Column(name = "end_lat", precision = 10, scale = 7)
  private BigDecimal endLat;

  @Column(name = "end_lng", precision = 10, scale = 7)
  private BigDecimal endLng;

  @Column(name = "total_distance_m")
  private Integer totalDistanceM;

  @Column(name = "travel_date")
  private LocalDate travelDate;

  @Column(name = "is_completed")
  private boolean isCompleted = false;

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @OrderBy("visitOrder ASC")
  private List<CoursePlace> coursePlaces = new ArrayList<>();

  protected TravelCourse() {}

  public TravelCourse(
      UUID userId,
      String startLocation,
      BigDecimal startLat,
      BigDecimal startLng,
      String endLocation,
      BigDecimal endLat,
      BigDecimal endLng,
      LocalDate travelDate) {
    this.userId = userId;
    this.startLocation = startLocation;
    this.startLat = startLat;
    this.startLng = startLng;
    this.endLocation = endLocation;
    this.endLat = endLat;
    this.endLng = endLng;
    this.travelDate = travelDate;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getStartLocation() {
    return startLocation;
  }

  public BigDecimal getStartLat() {
    return startLat;
  }

  public BigDecimal getStartLng() {
    return startLng;
  }

  public String getEndLocation() {
    return endLocation;
  }

  public BigDecimal getEndLat() {
    return endLat;
  }

  public BigDecimal getEndLng() {
    return endLng;
  }

  public Integer getTotalDistanceM() {
    return totalDistanceM;
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

  public void complete() {
    this.isCompleted = true;
  }
}
