package com.pettrip.wishlist.service;

import com.pettrip.common.service.NotFoundException;

public class WishlistItemNotFoundException extends NotFoundException {

  public WishlistItemNotFoundException() {
    super("위시리스트 항목을 찾을 수 없습니다.");
  }
}
