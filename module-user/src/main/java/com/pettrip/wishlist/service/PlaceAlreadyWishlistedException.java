package com.pettrip.wishlist.service;

import com.pettrip.common.service.ConflictException;

public class PlaceAlreadyWishlistedException extends ConflictException {

  public PlaceAlreadyWishlistedException() {
    super("이미 위시리스트에 있는 장소입니다.");
  }
}
