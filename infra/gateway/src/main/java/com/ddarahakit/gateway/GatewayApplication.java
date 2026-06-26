package com.ddarahakit.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** API 게이트웨이 — 인증의 단일 관문(JWT 검증·신원 전파)이자 라우팅/디스커버리 진입점. */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
