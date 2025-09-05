
// Also check your ReviewRequestDto has proper validation
package com.moviehub.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequestDto {

    @NotBlank(message = "Movie selection is required")
    private String movieId;

    private String userId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @NotBlank(message = "Comment is required")
    @Size(min = 5, max = 1000, message = "Comment must be between 10 and 1000 characters")
    private String comment;
}