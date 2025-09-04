package com.moviehub.review.api;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.dto.ReviewRequestDto;
import com.moviehub.review.dto.ReviewResponseDto;
import com.moviehub.review.service.MovieService;
import com.moviehub.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private ReviewService reviewService;

    // Movie API endpoints
    @GetMapping("/movies")
    public Flux<MovieResponseDto> getAllMovies() {
        return movieService.getALlMovies();
    }

    @GetMapping("/movies/{id}")
    public Mono<ResponseEntity<MovieResponseDto>> getMovieById(@PathVariable String id) {
        return movieService.getMovieById(id)
                .map(movie -> ResponseEntity.ok(movie))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/movies")
    public Mono<ResponseEntity<MovieResponseDto>> createMovie(@Valid @RequestBody MovieRequestDto movieRequest) {
        return movieService.createMovie(movieRequest)
                .map(movie -> ResponseEntity.status(HttpStatus.CREATED).body(movie));
    }

    @PutMapping("/movies/{id}")
    public Mono<ResponseEntity<MovieResponseDto>> updateMovie(@PathVariable String id,
                                                              @Valid @RequestBody MovieRequestDto movieRequest) {
        return movieService.updateMovie(id, movieRequest)
                .map(movie -> ResponseEntity.ok(movie))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/movies/{id}")
    public Mono<ResponseEntity<Void>> deleteMovie(@PathVariable String id) {
        return movieService.deleteMovie(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Review API endpoints
    @GetMapping("/reviews")
    public Flux<ReviewResponseDto> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @GetMapping("/reviews/{id}")
    public Mono<ResponseEntity<ReviewResponseDto>> getReviewById(@PathVariable String id) {
        return reviewService.getReviewById(id)
                .map(review -> ResponseEntity.ok(review))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/movies/{movieId}/reviews")
    public Flux<ReviewResponseDto> getReviewsByMovieId(@PathVariable String movieId) {
        return reviewService.getReviewsByMovieId(movieId);
    }

    @PostMapping("/reviews")
    public Mono<ResponseEntity<ReviewResponseDto>> createReview(@Valid @RequestBody ReviewRequestDto reviewRequest) {
        return reviewService.createReview(reviewRequest)
                .map(review -> ResponseEntity.status(HttpStatus.CREATED).body(review));
    }

    @PutMapping("/reviews/{id}")
    public Mono<ResponseEntity<ReviewResponseDto>> updateReview(@PathVariable String id,
                                                                @Valid @RequestBody ReviewRequestDto reviewRequest) {
        return reviewService.updateReview(reviewRequest, id)
                .map(review -> ResponseEntity.ok(review))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/reviews/{id}")
    public Mono<ResponseEntity<Void>> deleteReview(@PathVariable String id) {
        return reviewService.deleteReview(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Statistics endpoints
    @GetMapping("/movies/{movieId}/average-rating")
    public Mono<ResponseEntity<Double>> getMovieAverageRating(@PathVariable String movieId) {
        return reviewService.calculateAverageRatingForMovie(movieId)
                .map(rating -> ResponseEntity.ok(rating));
    }

    @GetMapping("/movies/search")
    public Flux<MovieResponseDto> searchMovies(@RequestParam(required = false) String genre,
                                               @RequestParam(required = false) String title,
                                               @RequestParam(required = false) Integer year) {
        if (genre != null) {
            return movieService.findMoviesByGenre(genre);
        } else if (title != null) {
            // You would implement this in your service
            return movieService.getALlMovies()
                    .filter(movie -> movie.getTitle().toLowerCase().contains(title.toLowerCase()));
        } else if (year != null) {
            return movieService.getALlMovies()
                    .filter(movie -> movie.getReleaseYear().equals(year));
        } else {
            return movieService.getALlMovies();
        }
    }
}