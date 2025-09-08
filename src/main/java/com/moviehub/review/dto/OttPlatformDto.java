package com.moviehub.review.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OttPlatformDto {
    private String platformName;
    private String availabilityRegion;
    private LocalDate availableFrom;
    private String streamingUrl;
    private String subscriptionType;
}
