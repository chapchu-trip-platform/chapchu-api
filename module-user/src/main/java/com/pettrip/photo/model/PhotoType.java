package com.pettrip.photo.model;

/** 사진이 쓰이는 기능. S3 키의 최상위 폴더로 사용되어 기능별 정리·수명주기 관리를 돕는다. */
public enum PhotoType {
  VISIT("visit"),
  POST("post"),
  ALBUM("album"),
  PROFILE("profile");

  private final String folder;

  PhotoType(String folder) {
    this.folder = folder;
  }

  public String folder() {
    return folder;
  }
}
