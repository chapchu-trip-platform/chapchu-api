package com.pettrip.main.service;

import com.pettrip.pet.model.Pet;
import com.pettrip.pet.repository.PetRepository;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.UserRepository;
import com.pettrip.user.service.UserNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 날씨/추천 여행 코스는 각각 이정범 담당 도메인(weather, trip)이 구현되기 전까지 이 요약에 포함하지 않는다. 담당 도메인 구현 후 이 서비스에서 조합하도록
 * 확장한다.
 */
@Service
public class MainPageService {

  private final UserRepository userRepository;
  private final PetRepository petRepository;

  public MainPageService(UserRepository userRepository, PetRepository petRepository) {
    this.userRepository = userRepository;
    this.petRepository = petRepository;
  }

  public MainPageSummary getMainPage(UUID userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    var petNames = petRepository.findByUserId(userId).stream().map(Pet::getPetName).toList();
    return new MainPageSummary(user.getNickname(), petNames);
  }
}
