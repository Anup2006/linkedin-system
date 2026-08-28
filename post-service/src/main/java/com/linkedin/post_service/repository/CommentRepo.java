package com.linkedin.post_service.repository;

import com.linkedin.post_service.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepo extends JpaRepository<Comment,String> {
    List<Comment> findByPostIdAndOrderByCreatedAtDesc(String postId);

}
