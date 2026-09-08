package com.pettrip.post.controller;

import java.util.List;

public record PostListResponse(List<PostSummaryResponse> posts, String nextCursor) {}
