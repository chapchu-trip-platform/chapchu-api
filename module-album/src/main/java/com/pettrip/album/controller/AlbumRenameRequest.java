package com.pettrip.album.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumRenameRequest(@NotBlank @Size(max = 30) String albumName) {}
