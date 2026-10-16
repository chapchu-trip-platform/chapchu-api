package com.pettrip.pet.repository;

import com.pettrip.pet.model.PetActivity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetActivityRepository extends JpaRepository<PetActivity, UUID> {

  List<PetActivity> findAllByOrderByActivityNameAsc();
}
