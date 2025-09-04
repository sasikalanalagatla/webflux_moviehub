package com.moviehub.review.repository;

import com.moviehub.review.model.Review;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ReviewRepository extends ReactiveMongoRepository<Review, String> {
    Flux<Review> findByMovieId(String movieId);
}
