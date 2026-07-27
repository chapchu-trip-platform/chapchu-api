package com.pettrip.user.repository;

import com.pettrip.user.model.Region;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, UUID> {}
