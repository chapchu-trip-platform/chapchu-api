package com.pettrip.trip.repository;

import com.pettrip.trip.model.TravelCourse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelCourseRepository extends JpaRepository<TravelCourse, UUID> {

  @Query(
      "SELECT DISTINCT c FROM TravelCourse c"
          + " LEFT JOIN FETCH c.startCourse"
          + " LEFT JOIN FETCH c.coursePlaces"
          + " WHERE c.userId = :userId"
          + " ORDER BY c.createdAt DESC")
  List<TravelCourse> findByUserIdWithPlaces(@Param("userId") UUID userId);
}
