package com.pettrip.user.repository;

import com.pettrip.user.model.TransportMethod;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportMethodRepository extends JpaRepository<TransportMethod, UUID> {}
