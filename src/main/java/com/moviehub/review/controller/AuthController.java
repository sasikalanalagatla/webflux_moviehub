package com.moviehub.review.controller;

import com.moviehub.review.dto.UserRegistrationDto;
import com.moviehub.review.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public Mono<String> showLoginPage(@RequestParam(required = false) String error,
                                      @RequestParam(required = false) String logout,
                                      @RequestParam(required = false) String success,
                                      Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        if (success != null) {
            model.addAttribute("success", success); // Display the decoded message
        }
        return Mono.just("login");
    }

    @GetMapping("/register")
    public Mono<String> showRegistrationPage(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return Mono.just("register");
    }

    @PostMapping("/register")
    public Mono<String> registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userRegistrationDto,
                                     BindingResult bindingResult,
                                     Model model) {
        logger.info("Registration attempt for username: {}", userRegistrationDto.getUsername());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in registration form");
            return Mono.just("register");
        }

        return userService.registerUser(userRegistrationDto)
                .then(Mono.fromCallable(() -> {
                    // URL encode the success message
                    try {
                        String encodedMessage = URLEncoder.encode("Registration successful! Please login.", StandardCharsets.UTF_8.toString());
                        return "redirect:/login?success=" + encodedMessage;
                    } catch (UnsupportedEncodingException e) {
                        logger.error("Failed to encode success message", e);
                        return "redirect:/login?success=Registration+successful";
                    }
                }))
                .onErrorResume(error -> {
                    logger.error("Registration failed: {}", error.getMessage());
                    model.addAttribute("error", error.getMessage());
                    model.addAttribute("user", userRegistrationDto);
                    return Mono.just("register");
                });
    }

    @GetMapping("/")
    public Mono<String> home() {
        return Mono.just("redirect:/movie/");
    }

    @GetMapping("/access-denied")
    public Mono<String> accessDenied() {
        return Mono.just("access-denied");
    }
}
