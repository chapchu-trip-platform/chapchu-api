package com.pettrip.wishlist.service;

import com.pettrip.wishlist.model.Wishlist;
import com.pettrip.wishlist.repository.WishlistRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

  private final WishlistRepository wishlistRepository;

  public WishlistService(WishlistRepository wishlistRepository) {
    this.wishlistRepository = wishlistRepository;
  }

  public List<Wishlist> listMyWishlist(UUID userId) {
    return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  @Transactional
  public void removeFromWishlist(UUID userId, String placeId) {
    if (!wishlistRepository.existsByUserIdAndPlaceId(userId, placeId)) {
      throw new WishlistItemNotFoundException();
    }
    wishlistRepository.deleteByUserIdAndPlaceId(userId, placeId);
  }
}
