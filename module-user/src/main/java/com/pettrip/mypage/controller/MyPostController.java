package com.pettrip.mypage.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.mypage.service.MyPostService;
import com.pettrip.post.controller.PostResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/posts")
public class MyPostController {

  private final MyPostService myPostService;

  public MyPostController(MyPostService myPostService) {
    this.myPostService = myPostService;
  }

  @GetMapping
  public List<PostResponse> listMyPosts(@CurrentUserId UUID userId) {
    return myPostService.listMyPosts(userId);
  }
}
