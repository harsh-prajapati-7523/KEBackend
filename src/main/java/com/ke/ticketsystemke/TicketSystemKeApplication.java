package com.ke.ticketsystemke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TicketSystemKeApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                TicketSystemKeApplication.class,
                args
        );
    }
}
