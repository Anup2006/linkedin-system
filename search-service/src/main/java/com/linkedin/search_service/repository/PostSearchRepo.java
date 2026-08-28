package com.linkedin.search_service.repository;

import com.linkedin.search_service.model.PostDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PostSearchRepo extends ElasticsearchRepository<PostDocument,String> {

    @Query("{\"match\": {\"content\": {\"query\": \"?0\", " +
            "\"fuzziness\": \"AUTO\"}}}")
    List<PostDocument> searchPosts(String query);

}
