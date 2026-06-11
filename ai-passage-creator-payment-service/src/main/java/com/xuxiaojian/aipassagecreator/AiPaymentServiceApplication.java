package com.xuxiaojian.aipassagecreator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.xuxiaojian.aipassagecreator")
@MapperScan("com.xuxiaojian.aipassagecreator.mapper")
@EnableFeignClients(basePackages = "com.xuxiaojian.aipassagecreator.api")
@EnableScheduling
public class AiPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPaymentServiceApplication.class, args);
    }
}
