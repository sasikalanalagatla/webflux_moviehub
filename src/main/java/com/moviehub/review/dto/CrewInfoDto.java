package com.moviehub.review.dto;

import lombok.Data;

import java.util.List;

@Data
public class CrewInfoDto {
    private List<CrewMemberDto> directors;
    private List<CrewMemberDto> producers;
    private List<CrewMemberDto> writers;
    private List<CrewMemberDto> choreographers;
    private List<CrewMemberDto> cinematographers;
    private List<CrewMemberDto> editors;
    private List<CrewMemberDto> musicDirectors;
}
