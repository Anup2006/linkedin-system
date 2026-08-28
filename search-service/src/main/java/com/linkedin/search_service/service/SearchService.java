package com.linkedin.search_service.service;

import com.linkedin.search_service.model.PostDocument;
import com.linkedin.search_service.model.UserDocument;
import com.linkedin.search_service.repository.PostSearchRepo;
import com.linkedin.search_service.repository.UserSearchRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {
    private final UserSearchRepo userSearchRepo;
    private final PostSearchRepo postSearchRepo;


    public List<UserDocument> searchUsers(String query) {
        log.info("Searching users: {}",query);
        return userSearchRepo.searchUsers(query);
    }

    public List<UserDocument> searchBySkill(String skill) {
        log.info("Searching users by skills: {}",skill);
        return userSearchRepo.findBySkillsContaining(skill);

    }

    public List<PostDocument> searchPosts(String query) {
        log.info("Searching posts: {}",query);
        return postSearchRepo.searchPosts(query);
    }
}
