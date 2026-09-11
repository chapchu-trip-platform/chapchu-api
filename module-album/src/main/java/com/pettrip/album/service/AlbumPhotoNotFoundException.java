package com.pettrip.album.service;

import com.pettrip.common.service.NotFoundException;

public class AlbumPhotoNotFoundException extends NotFoundException {

  public AlbumPhotoNotFoundException() {
    super("앨범에 없는 사진입니다.");
  }
}
