package com.pettrip.album.controller;

import java.util.UUID;

/**
 * 앨범은 유저당 하나다. 그 안의 사진을 반려동물의 {@code isDie}로 두 묶음으로 나눠 내려준다.
 *
 * @param album {@code isDie=false}인 아이들의 사진
 * @param memorialAlbum {@code isDie=true}인 아이들의 사진
 */
public record AlbumResponse(
    UUID albumId, String albumName, AlbumGroupResponse album, AlbumGroupResponse memorialAlbum) {}
