package com.linkedin.search_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

//document for post search
//Indexed when post is created
//enables full text search post content
@Document(indexName = "posts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Keyword)
    private String authorId;

    private String imageUrl;

    private String createdAt;
}
