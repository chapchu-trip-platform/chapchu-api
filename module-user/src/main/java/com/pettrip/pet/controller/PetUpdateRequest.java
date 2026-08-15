package com.pettrip.pet.controller;

import com.pettrip.pet.model.PetSize;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;

/**
 * @param activityIds 선호 활동. 생략({@code null})하면 기존 값을 유지하고, 빈 배열을 보내면 전부 지운다.
 */
public record PetUpdateRequest(
    String petName, Integer breedId, PetSize size, @Min(0) Integer age, List<UUID> activityIds) {}
