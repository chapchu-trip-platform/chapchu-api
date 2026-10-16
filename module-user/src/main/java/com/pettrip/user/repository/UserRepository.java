package com.pettrip.user.repository;

import com.pettrip.user.model.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByNickname(String nickname);

  /** 통합 회원가입에서 이미 가입한 계정인지 먼저 거른다. 구글 계정당 하나의 유저만 존재한다. */
  boolean existsByGoogleUserId(String googleUserId);
}
