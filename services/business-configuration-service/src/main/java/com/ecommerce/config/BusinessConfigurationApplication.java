package com.ecommerce.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.ecommerce.config", "com.ecommerce.common"})
public class BusinessConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessConfigurationApplication.class, args);
    }
}
