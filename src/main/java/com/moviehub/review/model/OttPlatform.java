package com.moviehub.review.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OttPlatform {
    private String platformName;
    private String availabilityRegion;
    private LocalDate availableFrom;
    private String streamingUrl;
    private String subscriptionType;
}
