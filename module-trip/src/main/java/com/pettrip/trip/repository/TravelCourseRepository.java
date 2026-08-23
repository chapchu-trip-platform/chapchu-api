package com.pettrip.trip.repository;

import com.pettrip.trip.model.TravelCourse;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelCourseRepository extends JpaRepository<TravelCourse, UUID> {}
