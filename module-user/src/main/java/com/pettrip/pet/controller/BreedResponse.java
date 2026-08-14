package com.pettrip.pet.controller;

import com.pettrip.pet.model.Breed;

public record BreedResponse(Integer id, String name) {

  public static BreedResponse from(Breed breed) {
    return new BreedResponse(breed.getId(), breed.getBreedName());
  }
}
