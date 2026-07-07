package com.pettrip.user.service;

import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User getMe(UUID userId) {
    return findUser(userId);
  }

  public User registerNickname(UUID userId, String nickname) {
    User user = findUser(userId);
    if (user.hasNickname()) {
      throw new NicknameAlreadyRegisteredException();
    }
    user.registerNickname(nickname);
    return userRepository.save(user);
  }

  public User updateMe(UUID userId, String nickname, AccountStatus accountStatus) {
    User user = findUser(userId);
    user.update(nickname, accountStatus);
    return userRepository.save(user);
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
  }
}
