package com.pettrip.place.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "place_pet_policies")
@EntityListeners(AuditingEntityListener.class)
public class PlacePetPolicy implements Persistable<String> {

  @Id
  @Column(name = "external_place_id", length = 255)
  private String externalPlaceId;

  @Transient private boolean isNew = true;

  @OneToOne
  @MapsId
  @JoinColumn(name = "external_place_id")
  private Place place;

  @Enumerated(EnumType.STRING)
  @Column(name = "allowed_pet_size", length = 10)
  private AllowedPetSize allowedPetSize;

  @Column(name = "leash_required")
  private Boolean leashRequired;

  @Column(name = "carrier_required")
  private Boolean carrierRequired;

  @Enumerated(EnumType.STRING)
  @Column(name = "indoor_outdoor_type", length = 20)
  private IndoorOutdoorType indoorOutdoorType;

  @Column private Boolean parking;

  @Column(name = "place_caution", columnDefinition = "TEXT")
  private String placeCaution;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected PlacePetPolicy() {}

  public PlacePetPolicy(
      Place place,
      AllowedPetSize allowedPetSize,
      Boolean leashRequired,
      Boolean carrierRequired,
      IndoorOutdoorType indoorOutdoorType,
      Boolean parking,
      String placeCaution) {
    this.place = place;
    this.externalPlaceId = place.getExternalPlaceId();
    this.allowedPetSize = allowedPetSize;
    this.leashRequired = leashRequired;
    this.carrierRequired = carrierRequired;
    this.indoorOutdoorType = indoorOutdoorType;
    this.parking = parking;
    this.placeCaution = placeCaution;
  }

  public void update(
      AllowedPetSize allowedPetSize,
      Boolean leashRequired,
      Boolean carrierRequired,
      IndoorOutdoorType indoorOutdoorType,
      Boolean parking,
      String placeCaution) {
    if (allowedPetSize != null) this.allowedPetSize = allowedPetSize;
    if (leashRequired != null) this.leashRequired = leashRequired;
    if (carrierRequired != null) this.carrierRequired = carrierRequired;
    if (indoorOutdoorType != null) this.indoorOutdoorType = indoorOutdoorType;
    if (parking != null) this.parking = parking;
    if (placeCaution != null) this.placeCaution = placeCaution;
  }

  @PostPersist
  @PostLoad
  void markNotNew() {
    this.isNew = false;
  }

  @Override
  public String getId() {
    return externalPlaceId;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  public String getExternalPlaceId() {
    return externalPlaceId;
  }

  public AllowedPetSize getAllowedPetSize() {
    return allowedPetSize;
  }

  public Boolean getLeashRequired() {
    return leashRequired;
  }

  public Boolean getCarrierRequired() {
    return carrierRequired;
  }

  public IndoorOutdoorType getIndoorOutdoorType() {
    return indoorOutdoorType;
  }

  public Boolean getParking() {
    return parking;
  }

  public String getPlaceCaution() {
    return placeCaution;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
