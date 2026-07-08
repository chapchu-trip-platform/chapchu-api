package com.pettrip.mypage.service;

import com.pettrip.pet.repository.PetRepository;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.UserRepository;
import com.pettrip.user.service.UserNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MyPageService {

  private final UserRepository userRepository;
  private final PetRepository petRepository;

  public MyPageService(UserRepository userRepository, PetRepository petRepository) {
    this.userRepository = userRepository;
    this.petRepository = petRepository;
  }

  public MyPageSummary getSummary(UUID userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    long petCount = petRepository.countByUserId(userId);
    return new MyPageSummary(user.getNickname(), user.getEmail(), petCount);
  }
}
