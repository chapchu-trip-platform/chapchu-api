package com.pettrip.pet.repository;

import com.pettrip.pet.model.Breed;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreedRepository extends JpaRepository<Breed, UUID> {}
