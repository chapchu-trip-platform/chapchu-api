package com.pettrip.place.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "places")
@EntityListeners(AuditingEntityListener.class)
public class Place {

  @Id
  @Column(name = "external_place_id", length = 255, updatable = false, nullable = false)
  private String externalPlaceId;

  @Column(name = "theme_id")
  private UUID themeId;

  @Column(name = "place_name", length = 100)
  private String placeName;

  @Column(name = "place_image_url", length = 500)
  private String placeImageUrl;

  @Column(length = 255)
  private String address;

  @Column(precision = 10, scale = 7)
  private BigDecimal latitude;

  @Column(precision = 10, scale = 7)
  private BigDecimal longitude;

  @Column(name = "business_hours", length = 255)
  private String businessHours;

  @Column(name = "phone_number", length = 30)
  private String phoneNumber;

  @Column private Short rating;

  @Column(name = "review_num")
  private Integer reviewNum = 0;

  @Column(name = "visit_num")
  private Integer visitNum = 0;

  @OneToOne(mappedBy = "place", cascade = CascadeType.ALL, optional = true)
  private PlacePetPolicy petPolicy;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected Place() {}

  public Place(
      String externalPlaceId,
      UUID themeId,
      String placeName,
      String placeImageUrl,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      String businessHours,
      String phoneNumber,
      Short rating) {
    this.externalPlaceId = externalPlaceId;
    this.themeId = themeId;
    this.placeName = placeName;
    this.placeImageUrl = placeImageUrl;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
    this.businessHours = businessHours;
    this.phoneNumber = phoneNumber;
    this.rating = rating;
  }

  public void update(
      UUID themeId,
      String placeName,
      String placeImageUrl,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      String businessHours,
      String phoneNumber,
      Short rating) {
    if (themeId != null) this.themeId = themeId;
    if (placeName != null) this.placeName = placeName;
    if (placeImageUrl != null) this.placeImageUrl = placeImageUrl;
    if (address != null) this.address = address;
    if (latitude != null) this.latitude = latitude;
    if (longitude != null) this.longitude = longitude;
    if (businessHours != null) this.businessHours = businessHours;
    if (phoneNumber != null) this.phoneNumber = phoneNumber;
    if (rating != null) this.rating = rating;
  }

  public void incrementReviewNum() {
    this.reviewNum++;
  }

  public void incrementVisitNum() {
    this.visitNum++;
  }

  public String getExternalPlaceId() {
    return externalPlaceId;
  }

  public UUID getThemeId() {
    return themeId;
  }

  public String getPlaceName() {
    return placeName;
  }

  public String getPlaceImageUrl() {
    return placeImageUrl;
  }

  public String getAddress() {
    return address;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public String getBusinessHours() {
    return businessHours;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public Short getRating() {
    return rating;
  }

  public Integer getReviewNum() {
    return reviewNum;
  }

  public Integer getVisitNum() {
    return visitNum;
  }

  public PlacePetPolicy getPetPolicy() {
    return petPolicy;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
