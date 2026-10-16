package com.pettrip.photo.service;

import java.time.LocalDate;
import java.util.UUID;

/** 사진 1장 저장 요청. coursePlaceId/takenAt은 선택. */
public record PhotoSaveCommand(UUID coursePlaceId, String photoKey, LocalDate takenAt) {}
