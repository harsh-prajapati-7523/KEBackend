package com.ke.ticketsystemke.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ke.ticketsystemke.security.JwtAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            logEmployeeManagementDenied(request.getRequestURI(), null);
                            response.sendError(401);
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            String actorEmployeeId = request.getUserPrincipal() == null
                                    ? null
                                    : request.getUserPrincipal().getName();
                            logEmployeeManagementDenied(request.getRequestURI(), actorEmployeeId);
                            response.sendError(403);
                        }))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/volt/auth/**")
                        .permitAll()

                        .requestMatchers(
                                "/volt/role-access",
                                "/volt/role-access/**")
                        .hasRole("SUPER_ADMIN")

                        .requestMatchers(
                                "/volt/workflow/transitions",
                                "/volt/workflow/transitions/**")
                        .hasRole("SUPER_ADMIN")

                        .requestMatchers(
                                "/volt/employees",
                                "/volt/employees/**")
                        .authenticated()

                        .requestMatchers(
                                "/volt/roles",
                                "/volt/roles/**")
                        .authenticated()

                        .requestMatchers(
                                "/volt/dropdown-sources",
                                "/volt/dropdown-sources/**")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/ticket-categories/*/form-fields")
                        .authenticated()

                        .requestMatchers(
                                "/volt/ticket-categories",
                                "/volt/ticket-categories/**")
                        .authenticated()

                        .requestMatchers(
                                "/volt/ticket-categories/*/field-configs",
                                "/volt/ticket-categories/*/field-configs/**")
                        .authenticated()

                        .requestMatchers(
                                "/volt/ticket-fields",
                                "/volt/ticket-fields/**")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/suggestions/**")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/volt/tickets")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/volt/tickets/*/pick",
                                "/volt/tickets/*/start-work",
                                "/volt/tickets/*/complete")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/volt/tickets/*/cancel")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/volt/tickets/*/warranty")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/tickets/*/charges")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/tickets/*/customer-history")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/tickets/*/dynamic-values")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/tickets/*/available-actions")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/tickets/query")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/tickets/search")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/volt/tickets/*/charges")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/volt/tickets/*/charges/*")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/volt/tickets")
                        .authenticated()

                        .anyRequest().authenticated())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                Arrays.stream(corsAllowedOrigins.split(","))
                        .map(String::trim)
                        .toList()
        );
        configuration.setAllowedMethods(
                Arrays.asList(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );
        configuration.setAllowedHeaders(
                Arrays.asList(
                        "Authorization",
                        "Content-Type"
                )
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private void logEmployeeManagementDenied(String endpoint, String actorEmployeeId) {
        if (endpoint.startsWith("/volt/employees")) {
            log.warn("event=employee_management_denied actorEmployeeId={} endpoint={} httpStatus={}",
                    actorEmployeeId, endpoint, actorEmployeeId == null ? 401 : 403);
        }
    }
}
