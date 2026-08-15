package com.pettrip.pet.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "pets")
@AttributeOverride(name = "id", column = @Column(name = "pet_id"))
public class Pet extends BaseEntity {

  @Column(name = "user_id")
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "breed_id", nullable = false)
  private Breed breed;

  @Column(name = "pet_name", nullable = false, length = 50)
  private String petName;

  @Enumerated(EnumType.STRING)
  @Column(name = "size", length = 10)
  private PetSize size;

  @Column(name = "age")
  private Integer age;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "pet_preferences_activities",
      joinColumns = @JoinColumn(name = "pet_id"),
      inverseJoinColumns = @JoinColumn(name = "activity_id"))
  private Set<PetActivity> preferredActivities = new HashSet<>();

  protected Pet() {}

  public Pet(UUID userId, Breed breed, String petName, PetSize size, Integer age) {
    this.userId = userId;
    this.breed = breed;
    this.petName = petName;
    this.size = size;
    this.age = age;
  }

  public void update(Breed newBreed, String newPetName, PetSize newSize, Integer newAge) {
    if (newBreed != null) {
      this.breed = newBreed;
    }
    if (newPetName != null) {
      this.petName = newPetName;
    }
    if (newSize != null) {
      this.size = newSize;
    }
    if (newAge != null) {
      this.age = newAge;
    }
  }

  /**
   * 선호 활동을 통째로 갈아끼운다.
   *
   * <p>{@code null}이면 손대지 않는다. 수정 요청에서 활동을 빼고 보낸 것과 "활동을 모두 지워달라"는 요청을 구분해야 하기 때문이다. 빈 목록을 보내면 전부
   * 지운다.
   */
  public void replaceActivities(Set<PetActivity> newActivities) {
    if (newActivities == null) {
      return;
    }
    this.preferredActivities.clear();
    this.preferredActivities.addAll(newActivities);
  }

  public UUID getUserId() {
    return userId;
  }

  public Breed getBreed() {
    return breed;
  }

  public String getPetName() {
    return petName;
  }

  public PetSize getSize() {
    return size;
  }

  public Integer getAge() {
    return age;
  }

  public Set<PetActivity> getPreferredActivities() {
    return preferredActivities;
  }
}
