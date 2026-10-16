package com.pettrip.trip.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_places")
@AttributeOverride(name = "id", column = @Column(name = "course_place_id"))
public class CoursePlace extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private TravelCourse course;

  // 중간 스탑은 places를 참조(값 세팅), 도착지(임의 사용자 선택)는 NULL이고 아래 비정규화 필드를 쓴다.
  @Column(name = "external_place_id", length = 255)
  private String externalPlaceId;

  // 도착지 전용: places에 없으므로 이름·좌표를 여기 직접 저장(중간 스탑은 NULL).
  @Column(name = "place_name", length = 255)
  private String placeName;

  @Column(name = "latitude", precision = 10, scale = 7)
  private BigDecimal latitude;

  @Column(name = "longitude", precision = 10, scale = 7)
  private BigDecimal longitude;

  @Column(name = "visit_order")
  private short visitOrder;

  @Column(name = "final_place")
  private boolean finalPlace = false;

  @Column(name = "is_visited")
  private boolean isVisited = false;

  @Column(name = "visited_at")
  private LocalDateTime visitedAt;

  @Column(name = "reason", columnDefinition = "TEXT")
  private String reason;

  // 반려견 가능 여부(표시용). 중간 스탑은 크기 필터를 통과하므로 true, 도착지는 요청 정책으로 계산(모르면 null).
  @Column(name = "pet_allowed")
  private Boolean petAllowed = Boolean.TRUE;

  protected CoursePlace() {}

  public CoursePlace(
      TravelCourse course, String externalPlaceId, short visitOrder, boolean finalPlace) {
    this(course, externalPlaceId, visitOrder, finalPlace, null);
  }

  public CoursePlace(
      TravelCourse course,
      String externalPlaceId,
      short visitOrder,
      boolean finalPlace,
      String reason) {
    this.course = course;
    this.externalPlaceId = externalPlaceId;
    this.visitOrder = visitOrder;
    this.finalPlace = finalPlace;
    this.reason = reason;
  }

  /**
   * 도착지 스탑. 사용자가 임의로 고른 고정 끝점이라 places에 없다 → external_place_id 없이 이름·좌표를 비정규화 저장한다. 항상 finalPlace.
   */
  public static CoursePlace destination(
      TravelCourse course,
      short visitOrder,
      String placeName,
      BigDecimal latitude,
      BigDecimal longitude,
      Boolean petAllowed,
      String reason) {
    CoursePlace cp = new CoursePlace(course, null, visitOrder, true, reason);
    cp.placeName = placeName;
    cp.latitude = latitude;
    cp.longitude = longitude;
    cp.petAllowed = petAllowed;
    return cp;
  }

  public TravelCourse getCourse() {
    return course;
  }

  public String getExternalPlaceId() {
    return externalPlaceId;
  }

  public String getPlaceName() {
    return placeName;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
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

  public String getReason() {
    return reason;
  }

  public Boolean getPetAllowed() {
    return petAllowed;
  }

  public void markVisited() {
    this.isVisited = true;
    this.visitedAt = LocalDateTime.now();
  }
}
