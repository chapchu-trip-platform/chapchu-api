package com.pettrip.wishlist.repository;

import com.pettrip.wishlist.model.Wishlist;
import com.pettrip.wishlist.model.WishlistId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, WishlistId> {

  List<Wishlist> findByUserIdOrderByCreatedAtDesc(UUID userId);

  boolean existsByUserIdAndPlaceId(UUID userId, String placeId);

  void deleteByUserIdAndPlaceId(UUID userId, String placeId);
}
