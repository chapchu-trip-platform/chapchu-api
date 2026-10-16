package com.pettrip.post.model;

/** 게시글 종류. 목록에서 이 값으로 걸러 본다. */
public enum PostType {
  /** 일반 글. 타입을 보내지 않으면 이 값으로 저장된다. */
  GENERAL,
  /** 여행후기. 다녀온 코스 이야기를 쓰는 글. */
  TRAVEL_REVIEW
}
