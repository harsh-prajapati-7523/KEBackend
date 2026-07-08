package com.ke.ticketsystemke.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    @Test
    void corsAllowsEmployeeProductionOriginAlongsideExistingDomains() {
        SecurityConfig securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(
                securityConfig,
                "corsAllowedOrigins",
                "http://localhost:5173,http://127.0.0.1:5173,https://kumar-electricals.com,"
                        + "https://www.kumar-electricals.com,https://test.kumar-electricals.com,"
                        + "https://employee.kumar-electricals.com"
        );

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/volt/auth/pin/status");
        request.addHeader("Origin", "https://employee.kumar-electricals.com");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactlyElementsOf(List.of(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "https://kumar-electricals.com",
                        "https://www.kumar-electricals.com",
                        "https://test.kumar-electricals.com",
                        "https://employee.kumar-electricals.com"
                ));
        assertThat(configuration.getAllowedMethods()).contains("POST", "OPTIONS");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}
