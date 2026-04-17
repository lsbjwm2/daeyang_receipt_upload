package com.daeyang.ReceiptUpload.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class MultiDBConfig {
	// ---------- MSSQL (app.datasource.mssql.* 사용) ----------
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
