package com.xuxiaojian.aipassagecreator.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.xuxiaojian.aipassagecreator")
@EnableFeignClients(basePackages = "com.xuxiaojian.aipassagecreator.api")
public class AiStatisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiStatisticsServiceApplication.class, args);
    }
}
