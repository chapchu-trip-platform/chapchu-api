package com.pettrip.album.repository;

import com.pettrip.album.model.AlbumPhoto;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumPhotoRepository extends JpaRepository<AlbumPhoto, UUID> {

  boolean existsByAlbumIdAndPhotoId(UUID albumId, UUID photoId);

  void deleteByAlbumIdAndPhotoId(UUID albumId, UUID photoId);

  long countByAlbumId(UUID albumId);
}
