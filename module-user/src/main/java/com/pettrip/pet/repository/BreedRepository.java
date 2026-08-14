package com.pettrip.pet.repository;

import com.pettrip.pet.model.Breed;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreedRepository extends JpaRepository<Breed, Integer> {

  List<Breed> findAllByOrderByBreedNameAsc();
}
