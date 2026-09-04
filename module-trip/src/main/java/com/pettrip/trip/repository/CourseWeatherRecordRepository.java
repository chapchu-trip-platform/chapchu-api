package com.pettrip.trip.repository;

import com.pettrip.trip.model.CourseWeatherRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseWeatherRecordRepository extends JpaRepository<CourseWeatherRecord, UUID> {

  List<CourseWeatherRecord> findByCourseIdOrderByWeatherDateDesc(UUID courseId);
}
