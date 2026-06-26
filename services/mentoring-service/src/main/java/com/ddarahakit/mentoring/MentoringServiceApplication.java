package com.ddarahakit.mentoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MentoringServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentoringServiceApplication.class, args);
    }
}
