package com.pettrip.pet.service;

import com.pettrip.pet.model.Pet;

/**
 * 반려동물 + 사진 뷰. 컨트롤러가 응답 DTO로 매핑한다.
 *
 * <p>{@code downloadUrl}을 만들려면 {@code PhotoService}가 필요해 {@code PetResponse.from(pet)} 같은 정적 팩토리로는
 * 조립할 수 없다. 서비스가 조립해 넘긴다.
 *
 * @param profilePhoto 사진이 없으면 null
 * @param backgroundPhoto 배경화면이 없으면 null
 */
public record PetDetail(Pet pet, PetPhotoView profilePhoto, PetPhotoView backgroundPhoto) {}
