package com.hostelcomplaint.controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.hostelcomplaint.dto.AuthResponse;
import com.hostelcomplaint.entity.AppUser;
import com.hostelcomplaint.repository.AppUserRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            AuthenticationManager authenticationManager,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (body.getUsername() == null || body.getPassword() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username and password are required");
        }

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            body.getUsername().trim().toLowerCase(),
                            body.getPassword()));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password");
        }

        // Change an existing session ID after authentication.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        AppUser user = userRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Account not found"));

        return new AuthResponse(user);
    }

    // Public registration always creates a STUDENT; users cannot choose roles.
    @PostMapping("/register/student")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerStudent(
            @RequestBody StudentRegistrationRequest body) {

        if (body.getUsername() == null
                || body.getUsername().isBlank()
                || body.getFullName() == null
                || body.getFullName().isBlank()
                || body.getPassword() == null
                || body.getPassword().length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name, username, and a password of at least 8 characters are required");
        }

        String username = body.getUsername().trim().toLowerCase();

        if (!username.matches("[a-z0-9._-]{3,80}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username must be 3-80 characters: letters, numbers, dots, underscores or hyphens");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Username is already in use");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName(body.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        user.setRole("STUDENT");
        user.setEnabled(true);

        return new AuthResponse(userRepository.save(user));
    }

    @GetMapping("/me")
    public AuthResponse currentUser(Authentication authentication) {
        AppUser user = userRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));

        return new AuthResponse(user);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return Map.of("message", "Logged out successfully");
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class StudentRegistrationRequest {
        private String fullName;
        private String username;
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}