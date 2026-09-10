package com.pettrip.stamp.repository;

import com.pettrip.stamp.model.UserStamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStampRepository extends JpaRepository<UserStamp, UUID> {

  Optional<UserStamp> findByUserIdAndStampId(UUID userId, UUID stampId);
}
