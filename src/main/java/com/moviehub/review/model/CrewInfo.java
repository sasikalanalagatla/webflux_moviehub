package com.moviehub.review.model;

import lombok.Data;

import java.util.List;

@Data
public class CrewInfo {
    private List<CrewMember> directors;
    private List<CrewMember> producers;
    private List<CrewMember> writers;
    private List<CrewMember> choreographers;
    private List<CrewMember> cinematographers;
    private List<CrewMember> editors;
    private List<CrewMember> musicDirectors;
}
