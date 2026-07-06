package com.pettrip.pet.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "breeds")
public class Breed extends BaseEntity {

  @Column(name = "breed_name", nullable = false, length = 30)
  private String breedName;

  protected Breed() {}

  public Breed(String breedName) {
    this.breedName = breedName;
  }

  public String getBreedName() {
    return breedName;
  }
}
