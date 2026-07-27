package com.pettrip.user.controller;

import com.pettrip.user.model.AccountStatus;

public record UserUpdateRequest(String nickname, AccountStatus accountStatus) {}
