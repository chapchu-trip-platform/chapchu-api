package com.pettrip.photo.model;

/**
 * 사진이 실제로 업로드되는 진입점. S3 키의 최상위 폴더로 사용되어 기능별 정리·수명주기 관리를 돕는다.
 *
 * <p>앨범·추천장소는 리뷰(REVIEW) 사진을 재사용하므로 별도 타입이 없고, 스탬프는 링크만 걸어 업로드가 없다.
 */
public enum PhotoType {
  PROFILE("profile"),
  POST("post"),
  REVIEW("review");

  private final String folder;

  PhotoType(String folder) {
    this.folder = folder;
  }

  public String folder() {
    return folder;
  }
}
