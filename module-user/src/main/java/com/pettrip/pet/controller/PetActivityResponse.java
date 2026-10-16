package com.pettrip.pet.controller;

import com.pettrip.pet.model.PetActivity;
import java.util.UUID;

public record PetActivityResponse(UUID id, String name) {

  public static PetActivityResponse from(PetActivity activity) {
    return new PetActivityResponse(activity.getId(), activity.getActivityName());
  }
}
