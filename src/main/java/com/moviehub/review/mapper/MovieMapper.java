package com.moviehub.review.mapper;

import com.moviehub.review.dto.*;
import com.moviehub.review.model.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MovieMapper {

    public static MovieResponseDto toDto(Movie movie) {
        if (movie == null) {
            return null;
        }

        MovieResponseDto dto = new MovieResponseDto();
        dto.setMovieId(movie.getMovieId());
        dto.setTitle(movie.getTitle());
        dto.setOriginalTitle(movie.getOriginalTitle());
        dto.setGenre(movie.getGenre());
        dto.setReleaseYear(movie.getReleaseYear());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setReleased(movie.getReleased());
        dto.setAverageRating(movie.getAverageRating());
        dto.setOverview(movie.getOverview());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setBackdropUrl(movie.getBackdropUrl());
        dto.setRuntime(movie.getRuntime());
        dto.setLanguage(movie.getLanguage());
        dto.setCountry(movie.getCountry());
        dto.setTmdbId(movie.getTmdbId());
        dto.setImdbId(movie.getImdbId());

        if (movie.getCast() != null) {
            dto.setCast(movie.getCast().stream()
                    .map(MovieMapper::castMemberToDto)
                    .collect(Collectors.toList()));
        }

        if (movie.getCrew() != null) {
            dto.setCrew(crewInfoToDto(movie.getCrew()));
        }

        if (movie.getOttPlatforms() != null) {
            dto.setOttPlatforms(movie.getOttPlatforms().stream()
                    .map(MovieMapper::ottPlatformToDto)
                    .collect(Collectors.toList()));
        }

        dto.setSingers(movie.getSingers());
        dto.setLyricists(movie.getLyricists());
        dto.setMusicDirectors(movie.getMusicDirectors());

        return dto;
    }

    public static Movie toEntity(MovieRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        Movie movie = new Movie();
        movie.setTitle(requestDto.getTitle());
        movie.setOriginalTitle(requestDto.getOriginalTitle());
        movie.setGenre(requestDto.getGenre());
        movie.setReleaseYear(requestDto.getReleaseYear());
        movie.setReleaseDate(requestDto.getReleaseDate());
        movie.setOverview(requestDto.getOverview());
        movie.setPosterUrl(requestDto.getPosterUrl());
        movie.setBackdropUrl(requestDto.getBackdropUrl());
        movie.setRuntime(requestDto.getRuntime());
        movie.setLanguage(requestDto.getLanguage());
        movie.setCountry(requestDto.getCountry());

        if (requestDto.getCast() != null) {
            movie.setCast(requestDto.getCast().stream()
                    .map(MovieMapper::castMemberToEntity)
                    .collect(Collectors.toList()));
        }

        if (requestDto.getCrew() != null) {
            movie.setCrew(crewInfoToEntity(requestDto.getCrew()));
        }

        if (requestDto.getOttPlatforms() != null) {
            movie.setOttPlatforms(requestDto.getOttPlatforms().stream()
                    .map(MovieMapper::ottPlatformToEntity)
                    .collect(Collectors.toList()));
        }

        movie.setSingers(requestDto.getSingers());
        movie.setLyricists(requestDto.getLyricists());
        movie.setMusicDirectors(requestDto.getMusicDirectors());

        return movie;
    }

    public static CastMemberDto castMemberToDto(CastMember castMember) {
        if (castMember == null) {
            return null;
        }

        CastMemberDto dto = new CastMemberDto();
        dto.setName(castMember.getName());
        dto.setCharacter(castMember.getCharacter());
        dto.setRole(castMember.getRole());
        dto.setProfileUrl(castMember.getProfileUrl());
        dto.setOrder(castMember.getOrder());
        return dto;
    }

    public static CastMember castMemberToEntity(CastMemberDto dto) {
        if (dto == null) {
            return null;
        }

        CastMember castMember = new CastMember();
        castMember.setName(dto.getName());
        castMember.setCharacter(dto.getCharacter());
        castMember.setRole(dto.getRole());
        castMember.setProfileUrl(dto.getProfileUrl());
        castMember.setOrder(dto.getOrder());
        return castMember;
    }

    public static CrewInfoDto crewInfoToDto(CrewInfo crewInfo) {
        if (crewInfo == null) {
            return null;
        }

        CrewInfoDto dto = new CrewInfoDto();

        if (crewInfo.getDirectors() != null) {
            dto.setDirectors(crewInfo.getDirectors().stream()
                    .map(MovieMapper::crewMemberToDto)
                    .collect(Collectors.toList()));
        }

        if (crewInfo.getProducers() != null) {
            dto.setProducers(crewInfo.getProducers().stream()
                    .map(MovieMapper::crewMemberToDto)
                    .collect(Collectors.toList()));
        }

        if (crewInfo.getWriters() != null) {
            dto.setWriters(crewInfo.getWriters().stream()
                    .map(MovieMapper::crewMemberToDto)
                    .collect(Collectors.toList()));
        }

        if (crewInfo.getChoreographers() != null) {
            dto.setChoreographers(crewInfo.getChoreographers().stream()
                    .map(MovieMapper::crewMemberToDto)
                    .collect(Collectors.toList()));
        }

        if (crewInfo.getCinematographers() != null) {
            dto.setCinematographers(crewInfo.getCinematographers().stream()
                    .map(MovieMapper::crewMemberToDto)
                    .collect(Collectors.toList()));
        }

        if (crewInfo.getEditors() != null) {
            dto.setEditors(crewInfo.getEditors().stream()
                    .map(MovieMapper::crewMemberToDto)
                    .collect(Collectors.toList()));
        }

        if (crewInfo.getMusicDirectors() != null) {
            dto.setMusicDirectors(crewInfo.getMusicDirectors().stream()
                    .map(MovieMapper::crewMemberToDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public static CrewInfo crewInfoToEntity(CrewInfoDto dto) {
        if (dto == null) {
            return null;
        }

        CrewInfo crewInfo = new CrewInfo();

        if (dto.getDirectors() != null) {
            crewInfo.setDirectors(dto.getDirectors().stream()
                    .map(MovieMapper::crewMemberToEntity)
                    .collect(Collectors.toList()));
        }

        if (dto.getProducers() != null) {
            crewInfo.setProducers(dto.getProducers().stream()
                    .map(MovieMapper::crewMemberToEntity)
                    .collect(Collectors.toList()));
        }

        if (dto.getWriters() != null) {
            crewInfo.setWriters(dto.getWriters().stream()
                    .map(MovieMapper::crewMemberToEntity)
                    .collect(Collectors.toList()));
        }

        if (dto.getChoreographers() != null) {
            crewInfo.setChoreographers(dto.getChoreographers().stream()
                    .map(MovieMapper::crewMemberToEntity)
                    .collect(Collectors.toList()));
        }

        if (dto.getCinematographers() != null) {
            crewInfo.setCinematographers(dto.getCinematographers().stream()
                    .map(MovieMapper::crewMemberToEntity)
                    .collect(Collectors.toList()));
        }

        if (dto.getEditors() != null) {
            crewInfo.setEditors(dto.getEditors().stream()
                    .map(MovieMapper::crewMemberToEntity)
                    .collect(Collectors.toList()));
        }

        if (dto.getMusicDirectors() != null) {
            crewInfo.setMusicDirectors(dto.getMusicDirectors().stream()
                    .map(MovieMapper::crewMemberToEntity)
                    .collect(Collectors.toList()));
        }

        return crewInfo;
    }

    public static CrewMemberDto crewMemberToDto(CrewMember crewMember) {
        if (crewMember == null) {
            return null;
        }

        CrewMemberDto dto = new CrewMemberDto();
        dto.setName(crewMember.getName());
        dto.setJob(crewMember.getJob());
        dto.setDepartment(crewMember.getDepartment());
        dto.setProfileUrl(crewMember.getProfileUrl());
        return dto;
    }

    public static CrewMember crewMemberToEntity(CrewMemberDto dto) {
        if (dto == null) {
            return null;
        }

        CrewMember crewMember = new CrewMember();
        crewMember.setName(dto.getName());
        crewMember.setJob(dto.getJob());
        crewMember.setDepartment(dto.getDepartment());
        crewMember.setProfileUrl(dto.getProfileUrl());
        return crewMember;
    }

    public static OttPlatformDto ottPlatformToDto(OttPlatform ottPlatform) {
        if (ottPlatform == null) {
            return null;
        }

        OttPlatformDto dto = new OttPlatformDto();
        dto.setPlatformName(ottPlatform.getPlatformName());
        dto.setAvailabilityRegion(ottPlatform.getAvailabilityRegion());
        dto.setAvailableFrom(ottPlatform.getAvailableFrom());
        dto.setStreamingUrl(ottPlatform.getStreamingUrl());
        dto.setSubscriptionType(ottPlatform.getSubscriptionType());
        return dto;
    }

    public static OttPlatform ottPlatformToEntity(OttPlatformDto dto) {
        if (dto == null) {
            return null;
        }

        OttPlatform ottPlatform = new OttPlatform();
        ottPlatform.setPlatformName(dto.getPlatformName());
        ottPlatform.setAvailabilityRegion(dto.getAvailabilityRegion());
        ottPlatform.setAvailableFrom(dto.getAvailableFrom());
        ottPlatform.setStreamingUrl(dto.getStreamingUrl());
        ottPlatform.setSubscriptionType(dto.getSubscriptionType());
        return ottPlatform;
    }

    public static List<MovieResponseDto> toDtoList(List<Movie> movies) {
        if (movies == null) {
            return Collections.emptyList();
        }
        return movies.stream()
                .map(MovieMapper::toDto)
                .collect(Collectors.toList());
    }

    public static List<Movie> toEntityList(List<MovieRequestDto> movieDtos) {
        if (movieDtos == null) {
            return Collections.emptyList();
        }
        return movieDtos.stream()
                .map(MovieMapper::toEntity)
                .collect(Collectors.toList());
    }
}
