package com.pettrip.user.service;

import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.Region;
import com.pettrip.user.model.Theme;
import com.pettrip.user.model.TransportMethod;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.RegionRepository;
import com.pettrip.user.repository.ThemeRepository;
import com.pettrip.user.repository.TransportMethodRepository;
import com.pettrip.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final RegionRepository regionRepository;
  private final ThemeRepository themeRepository;
  private final TransportMethodRepository transportMethodRepository;

  public UserService(
      UserRepository userRepository,
      RegionRepository regionRepository,
      ThemeRepository themeRepository,
      TransportMethodRepository transportMethodRepository) {
    this.userRepository = userRepository;
    this.regionRepository = regionRepository;
    this.themeRepository = themeRepository;
    this.transportMethodRepository = transportMethodRepository;
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

  public User getPreferences(UUID userId) {
    return findUser(userId);
  }

  public User updatePreferences(
      UUID userId, List<UUID> regionIds, List<UUID> themeIds, List<UUID> transportMethodIds) {
    User user = findUser(userId);
    if (regionIds != null) {
      user.replacePreferredRegions(new HashSet<>(findRegions(regionIds)));
    }
    if (themeIds != null) {
      user.replacePreferredThemes(new HashSet<>(findThemes(themeIds)));
    }
    if (transportMethodIds != null) {
      user.replacePreferredTransportMethods(
          new HashSet<>(findTransportMethods(transportMethodIds)));
    }
    return userRepository.save(user);
  }

  private List<Region> findRegions(List<UUID> ids) {
    List<Region> regions = regionRepository.findAllById(ids);
    if (regions.size() != ids.size()) {
      throw new RegionNotFoundException();
    }
    return regions;
  }

  private List<Theme> findThemes(List<UUID> ids) {
    List<Theme> themes = themeRepository.findAllById(ids);
    if (themes.size() != ids.size()) {
      throw new ThemeNotFoundException();
    }
    return themes;
  }

  private List<TransportMethod> findTransportMethods(List<UUID> ids) {
    List<TransportMethod> transportMethods = transportMethodRepository.findAllById(ids);
    if (transportMethods.size() != ids.size()) {
      throw new TransportMethodNotFoundException();
    }
    return transportMethods;
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
  }
}
