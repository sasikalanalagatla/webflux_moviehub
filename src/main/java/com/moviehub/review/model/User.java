package com.moviehub.review.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "users")
@AllArgsConstructor
@Data
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String role;
    private boolean enabled = true;
    private LocalDateTime createdAt;

    // Default constructor
    public User() {
        this.createdAt = LocalDateTime.now();
    }}
