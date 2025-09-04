package com.moviehub.review.exception.handler;

import com.moviehub.review.exception.MovieNotFoundException;
import com.moviehub.review.exception.ReviewNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(MovieNotFoundException.class)
    public String handleMovieNotFound(MovieNotFoundException ex, Model model) {
        model.addAttribute("error", "Movie not found: " + ex.getMessage());
        return "movies/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("error", "An unexpected error occurred: " + ex.getMessage());
        return "movies/error";
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public String handleReviewNotFound(ReviewNotFoundException ex, Model model) {
        model.addAttribute("error", "Review not found: " + ex.getMessage());
        return "movies/error";
    }
}