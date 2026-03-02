package com.avidreader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Produces a shared {@link RestTemplate} bean with explicit connection and read timeouts.
 * Timeout values are externalised to application.properties so they can be tuned per environment
 * without a code change.
 *
 * <pre>
 * http.client.connect-timeout-ms=3000
 * http.client.read-timeout-ms=5000
 * </pre>
 */
@Configuration
public class RestTemplateConfig {

    @Value("${http.client.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${http.client.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
