package com.pettrip.album.controller;

import java.util.List;

/** 앨범 한 묶음. 사진 수와 사진 목록을 담는다. */
public record AlbumGroupResponse(int photoCount, List<AlbumPhotoItem> photos) {}
