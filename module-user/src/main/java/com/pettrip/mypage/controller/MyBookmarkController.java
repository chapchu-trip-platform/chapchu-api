package com.pettrip.mypage.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.mypage.service.MyBookmarkService;
import com.pettrip.post.controller.PostResponse;
import java.util.List;
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
  public List<PostResponse> listMyBookmarks() {
    return myBookmarkService.listMyBookmarks(TempAuthContext.TEMP_USER_ID).stream()
        .map(PostResponse::from)
        .toList();
  }
}
