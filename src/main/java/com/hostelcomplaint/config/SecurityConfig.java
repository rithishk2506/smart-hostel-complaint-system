package com.hostelcomplaint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        CookieCsrfTokenRepository csrfRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        CsrfTokenRequestAttributeHandler csrfRequestHandler =
                new CsrfTokenRequestAttributeHandler();

        http
            // Enable CSRF protection for state-changing HTTP requests.
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(csrfRequestHandler)
            )

            // Generate the CSRF cookie when processing a request.
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)

            // Keep session-based authentication.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            .authorizeHttpRequests(auth -> auth

                // Public pages and static resources.
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/login.html",
                    "/favicon.ico",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()

                // Public authentication endpoints.
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register/student"
                ).permitAll()

                // Dashboard access by role.
                .requestMatchers("/student.html").hasRole("STUDENT")
                .requestMatchers("/staff.html").hasRole("STAFF")
                .requestMatchers("/warden.html").hasRole("WARDEN")
                .requestMatchers("/admin.html").hasRole("ADMIN")

                // Only administrators can manage privileged accounts.
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // All other endpoints require authentication.
                .anyRequest().authenticated()
            )

            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                    response.sendError(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Please log in to access this resource"
                    )
                )
                .accessDeniedHandler((request, response, exception) ->
                    response.sendError(
                        HttpStatus.FORBIDDEN.value(),
                        "Access denied: insufficient permissions or invalid CSRF token"
                    )
                )
            )

            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable());

        return http.build();
    }
}