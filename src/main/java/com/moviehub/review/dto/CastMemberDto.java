package com.moviehub.review.dto;

import lombok.Data;

@Data
public class CastMemberDto {
    private String name;
    private String character;
    private String role;
    private String profileUrl;
    private Integer order;
}
