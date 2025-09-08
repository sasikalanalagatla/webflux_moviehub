package com.moviehub.review.model;

import lombok.Data;

@Data
public class CastMember {
    private String name;
    private String character;
    private String role;
    private String profileUrl;
    private Integer order;
}
