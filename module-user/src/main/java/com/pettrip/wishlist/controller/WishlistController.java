package com.pettrip.wishlist.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.wishlist.service.WishlistService;
import java.util.List;
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
  public List<WishlistResponse> listMyWishlist() {
    return wishlistService.listMyWishlist(TempAuthContext.TEMP_USER_ID).stream()
        .map(WishlistResponse::from)
        .toList();
  }

  @DeleteMapping("/{placeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeFromWishlist(@PathVariable String placeId) {
    wishlistService.removeFromWishlist(TempAuthContext.TEMP_USER_ID, placeId);
  }
}
