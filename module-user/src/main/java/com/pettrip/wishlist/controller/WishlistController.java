package com.pettrip.wishlist.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.wishlist.service.WishlistService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/wishlist")
public class WishlistController {

  private final WishlistService wishlistService;

  public WishlistController(WishlistService wishlistService) {
    this.wishlistService = wishlistService;
  }

  @GetMapping
  public List<WishlistResponse> listMyWishlist(@CurrentUserId UUID userId) {
    return wishlistService.listMyWishlist(userId).stream().map(WishlistResponse::from).toList();
  }

  @DeleteMapping("/{placeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeFromWishlist(@CurrentUserId UUID userId, @PathVariable String placeId) {
    wishlistService.removeFromWishlist(userId, placeId);
  }
}
