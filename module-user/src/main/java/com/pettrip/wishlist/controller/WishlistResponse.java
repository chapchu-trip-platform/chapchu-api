package com.pettrip.wishlist.controller;

import com.pettrip.wishlist.model.Wishlist;
import java.time.LocalDateTime;

public record WishlistResponse(String placeId, LocalDateTime createdAt) {

  public static WishlistResponse from(Wishlist wishlist) {
    return new WishlistResponse(wishlist.getPlaceId(), wishlist.getCreatedAt());
  }
}
