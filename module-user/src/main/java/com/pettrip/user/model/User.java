package com.pettrip.user.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@AttributeOverride(name = "id", column = @Column(name = "user_id"))
public class User extends BaseEntity {

  @Column(name = "google_user_id", unique = true)
  private String googleUserId;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(length = 30)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_status", length = 20)
  private AccountStatus accountStatus;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_preference_regions",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "region_id"))
  private Set<Region> preferredRegions = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_preference_themes",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "theme_id"))
  private Set<Theme> preferredThemes = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_preference_transport_methods",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "transport_method_id"))
  private Set<TransportMethod> preferredTransportMethods = new HashSet<>();

  protected User() {}

  public User(String email, String googleUserId) {
    this.email = email;
    this.googleUserId = googleUserId;
    this.role = Role.USER;
    this.accountStatus = AccountStatus.ACTIVE;
  }

  public boolean hasNickname() {
    return this.nickname != null;
  }

  public void registerNickname(String newNickname) {
    this.nickname = newNickname;
  }

  public void update(String newNickname, AccountStatus newAccountStatus) {
    if (newNickname != null) {
      this.nickname = newNickname;
    }
    if (newAccountStatus != null) {
      this.accountStatus = newAccountStatus;
    }
  }

  public String getGoogleUserId() {
    return googleUserId;
  }

  public String getEmail() {
    return email;
  }

  public String getNickname() {
    return nickname;
  }

  public Role getRole() {
    return role;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public void replacePreferredRegions(Set<Region> regions) {
    this.preferredRegions.clear();
    this.preferredRegions.addAll(regions);
  }

  public void replacePreferredThemes(Set<Theme> themes) {
    this.preferredThemes.clear();
    this.preferredThemes.addAll(themes);
  }

  public void replacePreferredTransportMethods(Set<TransportMethod> transportMethods) {
    this.preferredTransportMethods.clear();
    this.preferredTransportMethods.addAll(transportMethods);
  }

  public Set<Region> getPreferredRegions() {
    return preferredRegions;
  }

  public Set<Theme> getPreferredThemes() {
    return preferredThemes;
  }

  public Set<TransportMethod> getPreferredTransportMethods() {
    return preferredTransportMethods;
  }
}
