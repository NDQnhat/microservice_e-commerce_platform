package com.ecommerce.exception;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ComponentScan(basePackages = {"com.ecommerce.exception", "com.ecommerce.common"})
public class ExceptionManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExceptionManagementApplication.class, args);
    }
}
