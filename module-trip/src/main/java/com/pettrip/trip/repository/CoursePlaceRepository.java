package com.pettrip.trip.repository;

import com.pettrip.trip.model.CoursePlace;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, UUID> {

  List<CoursePlace> findByCourseIdOrderByVisitOrderAsc(UUID courseId);
}
