package com.daeyang.ReceiptUpload;

import java.security.NoSuchAlgorithmException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
public class ReceiptUploadApplication {
	public static void main(String[] args) throws NoSuchAlgorithmException {
		SpringApplication.run(ReceiptUploadApplication.class, args);
	}
}
