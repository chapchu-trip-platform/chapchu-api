package com.pettrip.wishlist.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class WishlistId implements Serializable {

  private UUID userId;
  private String placeId;

  public WishlistId() {}

  public WishlistId(UUID userId, String placeId) {
    this.userId = userId;
    this.placeId = placeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WishlistId that)) {
      return false;
    }
    return Objects.equals(userId, that.userId) && Objects.equals(placeId, that.placeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, placeId);
  }
}
