package com.daeyang.SmartFactoryWeb.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class MultiDBConfig {
	// ---------- Primary: PostgreSQL (spring.datasource.* 사용) ----------
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties postgresProps() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource postgresDataSource(@Qualifier("postgresProps") DataSourceProperties props) {
        return props.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    JdbcTemplate postgresJdbcTemplate(@Qualifier("postgresDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
    
    @Bean
    @Primary
    NamedParameterJdbcTemplate postgresNamedJdbc(@Qualifier("postgresDataSource") DataSource ds) {
        return new NamedParameterJdbcTemplate(ds);
    }

    // ---------- Secondary: MSSQL (app.datasource.mssql.* 사용) ----------
    @Bean
    @ConfigurationProperties("app.datasource.mssql")
    DataSourceProperties mssqlProps() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.mssql.hikari")
    HikariDataSource mssqlDataSource(@Qualifier("mssqlProps") DataSourceProperties props) {
        return props.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    JdbcTemplate mssqlJdbcTemplate(@Qualifier("mssqlDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
    
    @Bean
    NamedParameterJdbcTemplate mssqlNamedJdbc(@Qualifier("mssqlDataSource") DataSource ds) {
        return new NamedParameterJdbcTemplate(ds);
    }
}
