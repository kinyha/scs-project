package com.pharmacy.scs.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * Тестовая конфигурация Spring.
 * Используется только для модульных тестов, не требующих TestContainers.
 */
@TestConfiguration
@Profile("test-h2")
public class TestConfig {

    /**
     * Создаем in-memory базу данных H2 для тестов
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}