package com.pettrip.pet.controller;

import com.pettrip.pet.model.PetSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * @param activityIds 선호 활동. 생략하면 등록하지 않는다. {@code GET /activities}로 선택지를 받는다.
 */
public record PetCreateRequest(
    @NotBlank String petName,
    @NotNull Integer breedId,
    @NotNull PetSize size,
    @NotNull @Min(0) Integer age,
    List<UUID> activityIds) {}
