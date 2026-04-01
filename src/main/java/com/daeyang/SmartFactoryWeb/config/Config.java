package com.daeyang.SmartFactoryWeb.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class Config {
	@Bean
	public RestTemplate restTemplate() {
	    CloseableHttpClient httpClient = HttpClients.createDefault();

	    HttpComponentsClientHttpRequestFactory factory =
	            new HttpComponentsClientHttpRequestFactory(httpClient);

	    factory.setConnectTimeout(Duration.ofSeconds(10));
	    factory.setReadTimeout(Duration.ofSeconds(30));

	    return new RestTemplate(factory);
	}
}
