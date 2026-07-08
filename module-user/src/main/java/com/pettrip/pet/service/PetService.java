package com.pettrip.pet.service;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.repository.BreedRepository;
import com.pettrip.pet.repository.PetRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PetService {

  private final PetRepository petRepository;
  private final BreedRepository breedRepository;

  public PetService(PetRepository petRepository, BreedRepository breedRepository) {
    this.petRepository = petRepository;
    this.breedRepository = breedRepository;
  }

  public List<Pet> listPets(UUID userId) {
    return petRepository.findByUserId(userId);
  }

  public Pet createPet(UUID userId, UUID breedId, String petName, PetSize size, Integer age) {
    Breed breed = findBreed(breedId);
    return petRepository.save(new Pet(userId, breed, petName, size, age));
  }

  public Pet updatePet(
      UUID userId, UUID petId, UUID breedId, String petName, PetSize size, Integer age) {
    Pet pet = getOwnedPet(userId, petId);
    Breed breed = breedId != null ? findBreed(breedId) : null;
    pet.update(breed, petName, size, age);
    return petRepository.save(pet);
  }

  public void deletePet(UUID userId, UUID petId) {
    Pet pet = getOwnedPet(userId, petId);
    petRepository.delete(pet);
  }

  private Breed findBreed(UUID breedId) {
    return breedRepository.findById(breedId).orElseThrow(BreedNotFoundException::new);
  }

  private Pet getOwnedPet(UUID userId, UUID petId) {
    Pet pet = petRepository.findById(petId).orElseThrow(PetNotFoundException::new);
    if (!pet.getUserId().equals(userId)) {
      throw new PetNotFoundException();
    }
    return pet;
  }
}
