package com.pettrip.post.controller;

import jakarta.validation.constraints.Size;

/** null인 필드는 그대로 둔다. {@link PostCreateRequest}와 같은 이유로 title 길이를 제한한다. */
public record PostUpdateRequest(@Size(max = 100) String title, String content) {}
