package com.fincen.sar.caseintegration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class CaseIntegrationConfig {

    @Value("${sar.case-management.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${sar.case-management.read-timeout:15000}")
    private int readTimeout;

    @Bean(name = "caseManagementRestTemplate")
    public RestTemplate caseManagementRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
