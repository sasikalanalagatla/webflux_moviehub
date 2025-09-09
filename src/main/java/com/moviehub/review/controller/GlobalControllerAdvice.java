package com.moviehub.review.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Global controller advice to automatically add common model attributes
 * to all controller responses across the application.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    /**
     * Automatically adds the current authenticated user to all models.
     * This eliminates the need to manually add authentication info in each controller method.
     *
     * @param principal The current authenticated user principal
     * @return The username if authenticated, null otherwise
     */
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