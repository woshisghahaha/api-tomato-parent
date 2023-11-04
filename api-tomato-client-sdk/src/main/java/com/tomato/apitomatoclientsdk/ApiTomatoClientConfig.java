package com.tomato.apitomatoclientsdk;


import com.tomato.apitomatoclientsdk.client.TomatoApiClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Configurable
@ConfigurationProperties("apitomato.client")
@Data
@Component
@ComponentScan
public class ApiTomatoClientConfig {

    private String accessKey;

    private String secretKey;

    @Bean
    public TomatoApiClient tomatoApiClient() {
        return new TomatoApiClient(accessKey, secretKey);
    }
}
