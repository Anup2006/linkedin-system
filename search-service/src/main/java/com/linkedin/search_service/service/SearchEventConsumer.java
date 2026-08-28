package com.linkedin.search_service.service;

import com.linkedin.search_service.model.PostDocument;
import com.linkedin.search_service.model.UserDocument;
import com.linkedin.search_service.repository.PostSearchRepo;
import com.linkedin.search_service.repository.UserSearchRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchEventConsumer {

    private final UserSearchRepo userSearchRepo;
    private final PostSearchRepo postSearchRepo;

    @KafkaListener(topics = "user.created")
    public void consumeUserCreated(
            @Payload Map<String,Object> payload
    ){
        try {
            log.info("Indexing new user: {}",payload.get("userId"));

            UserDocument document =new UserDocument();
            document.setId((String) payload.get("userId"));
            document.setFirstName((String) payload.get("firstName"));
            document.setLastName((String) payload.get("lastname"));
            document.setHeadline((String) payload.get("headline"));
            document.setEmail((String) payload.get("email"));
            document.setLocation((String) payload.get("location"));

            userSearchRepo.save(document);
            log.info("User indexed: {}",payload.get("userId"));

        } catch (Exception e) {
            log.error("Error indexing user: {}",e.getMessage());
        }
    }


    @KafkaListener(topics = "user.updated")
    public void consumeUserUpdated(
            @Payload Map<String,Object> payload
    ){
        try {
            String userId=(String) payload.get("userId");
            log.info("Updating user index: {}",userId);

            userSearchRepo.findById(userId).ifPresent(document->{
                document.setFirstName((String) payload.get("firstName"));
                document.setLastName((String) payload.get("lastname"));
                document.setHeadline((String) payload.get("headline"));
                document.setLocation((String) payload.get("location"));

                if(payload.get("skills")!=null){
                    document.setSkills((List<String>) payload.get("skills"));
                }

                userSearchRepo.save(document);
                log.info("User index updated: {}",userId);
            });

        } catch (Exception e) {
            log.error("Error updating user index: {}",e.getMessage());
        }
    }


    @KafkaListener(topics = "post.created")
    public void consumePostCreated(
            @Payload Map<String,Object> payload
    ){
        try {
            String postId=(String) payload.get("postId");
            log.info("Indexing new post: {}",postId);

            PostDocument document =new PostDocument();
            document.setId((String) payload.get("postId"));
            document.setContent((String) payload.get("content"));
            document.setAuthorId((String) payload.get("authorId"));
            document.setImageUrl((String) payload.get("imageUrl"));
            document.setCreatedAt((String) payload.get("createdAt"));

            postSearchRepo.save(document);

        } catch (Exception e) {
            log.error("Error indexing post: {}",e.getMessage());
        }
    }




}
