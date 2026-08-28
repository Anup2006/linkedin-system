package com.linkedin.post_service.repository;

import com.linkedin.post_service.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepo extends JpaRepository<Like,String> {
    boolean existsByPostIdAndUserId(String postId, String userId);

    Optional<Like> findByPostIdAndUserId(String postId, String userId);
}
