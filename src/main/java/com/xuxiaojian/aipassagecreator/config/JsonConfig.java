package com.xuxiaojian.aipassagecreator.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ClassName: JsonConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *   SpringMVC Json配置。
 *   这里通过定制 Jackson 构建器统一处理 Long 转字符串和 LocalDateTime 的格式化，
 *   这样能够直接作用于 Spring MVC 的消息转换器，避免仅定义 ObjectMapper 却没有被响应序列化链使用。
 * @Author 阿健
 * @Create 2026-05-26 23:09
 * @Version 1.0
 */
@Configuration
public class JsonConfig {

    /**
     * 统一的日期时间格式
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 定制 Jackson 构建器。
     * @return Jackson 构建器定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        };
    }
}
