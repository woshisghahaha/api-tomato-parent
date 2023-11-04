package com.tomato.apitomatointerface;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@EnableSwagger2WebMvc
@SpringBootApplication
public class ApiTomatoInterfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiTomatoInterfaceApplication.class, args);
    }

}
