package com.mealcheck.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * 날짜/시간 직렬화 설정.
 *
 * - LocalDate: 항상 yyyy-MM-dd 형식으로, 타임존 보정 없이 전송
 * - LocalDateTime: Asia/Seoul 기준 yyyy-MM-dd HH:mm:ss 형식으로 전송
 */
@Configuration
public class JacksonTimeConfig {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            JavaTimeModule javaTimeModule = new JavaTimeModule();

            // LocalDate는 순수 날짜만, 타임존 보정 없이 직렬화/역직렬화
            javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));
            javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));

            // LocalDateTime은 Asia/Seoul 기준으로 yyyy-MM-dd HH:mm:ss 사용
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));

            builder.modules(javaTimeModule);
            builder.timeZone(TimeZone.getTimeZone("Asia/Seoul"));
            builder.featuresToDisable(WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}


