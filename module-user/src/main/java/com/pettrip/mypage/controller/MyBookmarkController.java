package com.pettrip.mypage.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.mypage.service.MyBookmarkService;
import com.pettrip.post.controller.PostResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/bookmarks")
public class MyBookmarkController {

  private final MyBookmarkService myBookmarkService;

  public MyBookmarkController(MyBookmarkService myBookmarkService) {
    this.myBookmarkService = myBookmarkService;
  }

  @GetMapping
  public List<PostResponse> listMyBookmarks(@CurrentUserId UUID userId) {
    return myBookmarkService.listMyBookmarks(userId).stream().map(PostResponse::from).toList();
  }
}
