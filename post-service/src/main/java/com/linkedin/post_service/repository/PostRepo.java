package com.linkedin.post_service.repository;

import com.linkedin.post_service.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepo extends JpaRepository<Post,String> {

    List<Post> findByAuthorIdOrderByCreatedAtDesc(String userId);
}
