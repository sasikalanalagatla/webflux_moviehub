package com.moviehub.review.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document
@Data
public class Review {

    @Id
    private String reviewId;
    private String movieId;
    private String userId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
