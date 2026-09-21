package com.pettrip.user.controller;

/**
 * 내 정보 수정 요청. 두 필드 모두 선택이며, null이면 해당 항목을 건드리지 않는다.
 *
 * @param isWithdrawn 탈퇴 여부. true로 보내면 이후 토큰이 발급되지 않는다
 */
public record UserUpdateRequest(String nickname, Boolean isWithdrawn) {}
