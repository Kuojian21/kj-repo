package com.kj.repo.spring.boot.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author kj
 */
@Configuration
@SpringBootApplication
@ComponentScan(basePackages = {"com.kj.repo.spring.boot"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

}
