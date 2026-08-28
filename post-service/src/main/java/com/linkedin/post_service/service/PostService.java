package com.linkedin.post_service.service;

import com.linkedin.post_service.entity.Comment;
import com.linkedin.post_service.entity.Like;
import com.linkedin.post_service.entity.Post;
import com.linkedin.post_service.repository.CommentRepo;
import com.linkedin.post_service.repository.LikeRepo;
import com.linkedin.post_service.repository.PostRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {
    private final PostRepo postRepo;
    private final LikeRepo likeRepo;
    private final CommentRepo commentRepo;
    private final S3Service s3Service;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String POST_CREATED_TOPIC="post.created";
    private static final String POST_LIKED_TOPIC="post.liked";
    private static final String POST_COMMENTED_TOPIC="post.commented";

    //create a post
    //optionally upload image to s3
    //publish post.created event to kafka
    //feed and search service will consume this
    public Post createPost(String authorId, String content, MultipartFile image) {
        log.info("Creating post for user:{}", authorId);

        Post post= new Post();
        post.setAuthorId(authorId);
        post.setContent(content);

        if(image != null && !image.isEmpty()){
            String imageUrl=s3Service.uploadFile(
                    image,"posts/" + authorId
            );

            post.setImageUrl(imageUrl);
        }

        Post saved = postRepo.save(post);
        log.info("Post created:{}",saved.getId());

        //publish to kafka
        Map<String,Object> postCreatedEvent = new HashMap<>();
        postCreatedEvent.put("postId",saved.getId());
        postCreatedEvent.put("authorId",saved.getAuthorId());
        postCreatedEvent.put("content",saved.getContent());
        postCreatedEvent.put("imageUrl",saved.getImageUrl());
        postCreatedEvent.put("createdAt",saved.getCreatedAt().toString());

        kafkaTemplate.send(POST_CREATED_TOPIC,saved.getId(),postCreatedEvent);

        log.info("post created event published: {}", saved.getId());

        return saved;

    }

    public Post getPost(String postId) {
        return postRepo.findById(postId)
                .orElseThrow(()-> new RuntimeException("Post not found: "+postId));
    }

    public List<Post> getUserPosts(String userId) {
        return postRepo.findByAuthorIdAndOrderByCreatedAtDesc(userId);
    }


    public String likePost(String postId, String userId) {
        Post post = getPost(postId);

        if(likeRepo.existsByPostIdAndUserId(postId,userId)){
            //unlike
            likeRepo.findByPostIdAndUserId(postId,userId)
                    .ifPresent(likeRepo::delete);

            post.setLikeCount(post.getLikeCount()-1);
            postRepo.save(post);
            return "Post unliked";
        }

        //like
        Like like = new Like();
        like.setPostId(postId);
        like.setUserId(userId);
        likeRepo.save(like);
        post.setLikeCount(post.getLikeCount()+1);
        postRepo.save(post);

        //publish post.liked event
        Map<String,Object> postLikedEvent = new HashMap<>();
        postLikedEvent.put("postId",postId);
        postLikedEvent.put("userId",userId);
        postLikedEvent.put("authorId",post.getAuthorId());

        kafkaTemplate.send(POST_LIKED_TOPIC,postId,postLikedEvent);

        return "Post liked";
    }


    public Comment addComment(String postId, String authorId, String content) {
        Post post =getPost(postId);

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setContent(content);

        Comment saved = commentRepo.save(comment);

        post.setCommentCount(post.getCommentCount()+1);
        postRepo.save(post);

        //publish post.commented event
        Map<String,Object> postCommentedEvent = new HashMap<>();
        postCommentedEvent.put("postId",postId);
        postCommentedEvent.put("commentId",saved.getId());
        postCommentedEvent.put("authorId",authorId);
        postCommentedEvent.put("postAuthorId",post.getAuthorId());

        kafkaTemplate.send(POST_COMMENTED_TOPIC,postId,postCommentedEvent);

        return saved;
    }

    public List<Comment> getComments(String postId) {
        return commentRepo.findByPostIdAndOrderByCreatedAtDesc(postId);
    }

    public void deletePost(String postId, String userId) {
        Post post = getPost(postId);

        if(!post.getAuthorId().equals(userId)){
            throw new RuntimeException("Not authorized to delete this post");
        }
        postRepo.delete(post);
        log.info("Post deleted: {}",postId);
    }
}














