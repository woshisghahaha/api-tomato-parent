package com.tomato.apitomatointerface;

import com.tomato.apitomatoclientsdk.client.TomatoApiClient;
import com.tomato.apitomatoclientsdk.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ApiTomatoInterfaceApplicationTests {

    @Resource
    private TomatoApiClient tomatoApiClient;

    @Test
    void contextLoads() {

    }

}
