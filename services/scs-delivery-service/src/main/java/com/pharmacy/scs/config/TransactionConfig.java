package com.pharmacy.scs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    private final DataSource dataSource;

    public TransactionConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean(name = "transactionManager") // Здесь нужно явно указать имя бина
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }
}
