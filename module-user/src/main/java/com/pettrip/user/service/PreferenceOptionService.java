package com.pettrip.user.service;

import com.pettrip.user.model.Region;
import com.pettrip.user.model.Theme;
import com.pettrip.user.model.TransportMethod;
import com.pettrip.user.repository.RegionRepository;
import com.pettrip.user.repository.ThemeRepository;
import com.pettrip.user.repository.TransportMethodRepository;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * 선호 사항 등록/수정에 쓸 선택지를 제공한다.
 *
 * <p>{@code POST /users/me/preferences}는 지역/테마/이동수단을 <b>UUID로</b> 받는데, 클라이언트가 그 UUID를 얻을 방법이 없었다.
 * 코드값은 {@code V3__seed_preference_codes.sql}에서 {@code gen_random_uuid()}로 만들어지므로 하드코딩할 수도 없다. 이 조회가
 * 없으면 선호 사항 기능 자체를 쓸 수 없다.
 */
@Service
public class PreferenceOptionService {

  private final RegionRepository regionRepository;
  private final ThemeRepository themeRepository;
  private final TransportMethodRepository transportMethodRepository;

  public PreferenceOptionService(
      RegionRepository regionRepository,
      ThemeRepository themeRepository,
      TransportMethodRepository transportMethodRepository) {
    this.regionRepository = regionRepository;
    this.themeRepository = themeRepository;
    this.transportMethodRepository = transportMethodRepository;
  }

  public List<Region> findRegions() {
    List<Region> regions = new ArrayList<>(regionRepository.findAll());
    Collator collator = Collator.getInstance(Locale.KOREAN);
    regions.sort((a, b) -> collator.compare(a.getRegionName(), b.getRegionName()));
    return regions;
  }

  public List<Theme> findThemes() {
    List<Theme> themes = new ArrayList<>(themeRepository.findAll());
    Collator collator = Collator.getInstance(Locale.KOREAN);
    themes.sort((a, b) -> collator.compare(a.getThemeName(), b.getThemeName()));
    return themes;
  }

  public List<TransportMethod> findTransportMethods() {
    List<TransportMethod> methods = new ArrayList<>(transportMethodRepository.findAll());
    Collator collator = Collator.getInstance(Locale.KOREAN);
    methods.sort(
        (a, b) -> collator.compare(a.getTransportMethodName(), b.getTransportMethodName()));
    return methods;
  }
}
