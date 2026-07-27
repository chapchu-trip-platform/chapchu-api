package com.pettrip.photo.repository;

import com.pettrip.photo.model.Photo;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {}
