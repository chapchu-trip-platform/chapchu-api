package com.pettrip.album.repository;

import com.pettrip.album.model.Album;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, UUID> {

  Optional<Album> findByUserId(UUID userId);
}
