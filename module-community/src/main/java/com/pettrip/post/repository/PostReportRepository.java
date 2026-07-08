package com.pettrip.post.repository;

import com.pettrip.post.model.PostReport;
import com.pettrip.post.model.PostReportId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReportRepository extends JpaRepository<PostReport, PostReportId> {

  boolean existsByPostIdAndUserId(UUID postId, UUID userId);
}
