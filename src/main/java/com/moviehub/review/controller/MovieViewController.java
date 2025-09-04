package com.moviehub.review.controller;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.service.impl.MovieServiceImpl;
import com.moviehub.review.service.impl.ReviewServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/movie")
public class MovieViewController {

    @Autowired
    private MovieServiceImpl movieService;

    @Autowired
    private ReviewServiceImpl reviewService;

    @GetMapping("/")
    public Mono<String> home() {
        return Mono.just("home");
    }

    @GetMapping("/all")
    public Mono<String> getAllMovies(Model model,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) String genre,
                                     @RequestParam(required = false) Integer year) {

        if (search != null && !search.trim().isEmpty()) {
            return movieService.getALlMovies()
                    .filter(movie -> movie.getTitle().toLowerCase().contains(search.toLowerCase().trim()))
                    .collectList()
                    .doOnNext(movies -> {
                        model.addAttribute("movies", movies);
                        model.addAttribute("searchQuery", search);
                    })
                    .thenReturn("movie-list");
        } else if (genre != null && !genre.trim().isEmpty()) {
            return movieService.findMoviesByGenre(genre)
                    .collectList()
                    .doOnNext(movies -> {
                        model.addAttribute("movies", movies);
                        model.addAttribute("genreFilter", genre);
                    })
                    .thenReturn("movie-list");
        } else if (year != null) {
            return movieService.getALlMovies()
                    .filter(movie -> movie.getReleaseYear().equals(year))
                    .collectList()
                    .doOnNext(movies -> {
                        model.addAttribute("movies", movies);
                        model.addAttribute("yearFilter", year);
                    })
                    .thenReturn("movie-list");
        } else {
            return movieService.getALlMovies()
                    .collectList()
                    .doOnNext(movies -> model.addAttribute("movies", movies))
                    .thenReturn("movie-list");
        }
    }

    @GetMapping("/add")
    public Mono<String> showAddForm(Model model) {
        model.addAttribute("movie", new MovieRequestDto());
        return Mono.just("movie-form");
    }

    @GetMapping("/edit/{movieId}")
    public Mono<String> showEditForm(@PathVariable String movieId, Model model) {
        return movieService.getMovieById(movieId)
                .doOnNext(movie -> model.addAttribute("movie", movie))
                .thenReturn("movie-edit-form")
                .onErrorResume(error -> {
                    model.addAttribute("error", "Movie not found: " + error.getMessage());
                    return Mono.just("movies/error");
                });
    }

    @GetMapping("/{movieId}")
    public Mono<String> getMovieById(@PathVariable String movieId, Model model) {
        return movieService.getMovieById(movieId)
                .doOnNext(movie -> model.addAttribute("movie", movie))
                .flatMap(movie -> {
                    return reviewService.getReviewsByMovieId(movieId)
                            .collectList()
                            .doOnNext(reviews -> model.addAttribute("reviews", reviews));
                })
                .thenReturn("movie-detail")
                .onErrorResume(error -> {
                    model.addAttribute("error", "Movie not found: " + error.getMessage());
                    return Mono.just("movies/error");
                });
    }

    @PostMapping("/save")
    public Mono<String> createMovie(@Valid @ModelAttribute MovieRequestDto movieRequestDto,
                                    BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("movie", movieRequestDto);
            return Mono.just("movie-form");
        }

        return movieService.createMovie(movieRequestDto)
                .then(Mono.just("redirect:/movie/all"))
                .onErrorResume(error -> {
                    model.addAttribute("error", "Failed to create movie: " + error.getMessage());
                    model.addAttribute("movie", movieRequestDto);
                    return Mono.just("movie-form");
                });
    }

    @PostMapping("/update/{movieId}")
    public Mono<String> updateMovie(@PathVariable String movieId,
                                    @Valid @ModelAttribute MovieRequestDto movieRequestDto,
                                    BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return movieService.getMovieById(movieId)
                    .doOnNext(movie -> {
                        model.addAttribute("movie", movie);
                        model.addAttribute("errors", bindingResult.getAllErrors());
                    })
                    .thenReturn("movie-edit-form")
                    .onErrorReturn("movies/error");
        }

        return movieService.updateMovie(movieId, movieRequestDto)
                .then(Mono.just("redirect:/movie/all"))
                .onErrorResume(error -> {
                    model.addAttribute("error", "Failed to update movie: " + error.getMessage());
                    return movieService.getMovieById(movieId)
                            .doOnNext(movie -> model.addAttribute("movie", movie))
                            .thenReturn("movie-edit-form")
                            .onErrorReturn("movies/error");
                });
    }

    @PostMapping("/delete/{movieId}")
    public Mono<String> deleteMovie(@PathVariable String movieId) {
        return movieService.deleteMovie(movieId)
                .then(Mono.just("redirect:/movie/all"))
                .onErrorReturn("redirect:/movie/all");
    }
}