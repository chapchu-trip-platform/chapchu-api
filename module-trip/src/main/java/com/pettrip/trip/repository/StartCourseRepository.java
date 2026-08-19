package com.pettrip.trip.repository;

import com.pettrip.trip.model.StartCourse;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StartCourseRepository extends JpaRepository<StartCourse, UUID> {}
