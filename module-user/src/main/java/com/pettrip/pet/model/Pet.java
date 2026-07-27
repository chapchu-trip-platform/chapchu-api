package com.pettrip.pet.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
}
