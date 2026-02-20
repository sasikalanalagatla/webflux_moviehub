package com.moviehub.review.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    @ModelAttribute("currentUser")
    public String getCurrentUser(Principal principal) {
        if (principal != null) {
            logger.debug("Adding currentUser to model: {}", principal.getName());
            return principal.getName();
        }
        logger.debug("No authenticated user found");
        return null;
    }
}