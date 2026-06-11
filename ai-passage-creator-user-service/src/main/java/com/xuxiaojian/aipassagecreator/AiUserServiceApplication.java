package com.xuxiaojian.aipassagecreator;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.xuxiaojian.aipassagecreator")
@MapperScan("com.xuxiaojian.aipassagecreator.mapper")
@EnableFeignClients(basePackages = "com.xuxiaojian.aipassagecreator.api")
@Slf4j
public class AiUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiUserServiceApplication.class, args);
    }
}
