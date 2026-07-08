package com.pettrip.user.repository;

import com.pettrip.user.model.Theme;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThemeRepository extends JpaRepository<Theme, UUID> {}
