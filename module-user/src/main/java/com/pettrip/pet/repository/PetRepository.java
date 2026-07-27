package com.pettrip.pet.repository;

import com.pettrip.pet.model.Pet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, UUID> {

  List<Pet> findByUserId(UUID userId);

  long countByUserId(UUID userId);
}
