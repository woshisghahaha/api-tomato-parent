package com.tomato.apitomatoclientsdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.tomato.apitomatoclientsdk.model.User;

import java.util.HashMap;
import java.util.Map;

import static com.tomato.apitomatoclientsdk.utils.SignUtils.getSign;

public class testClient {
    public static void main(String[] args) {
        User user = new User();
        user.setUsername("tomato");
        getUsernameByPost(user);
    }
    private static Map<String, String> getHeaderMap(String body) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", "53349ea7504b8985606a8edbda11e314");
        // 一定不能直接发送
//        hashMap.put("secretKey", secretKey);
        hashMap.put("nonce", RandomUtil.randomNumbers(4));
        hashMap.put("body", body);
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        hashMap.put("sign", getSign(body, "82b9389e40dd4c589feebd12fe60786a"));
        return hashMap;
    }

    public static String getUsernameByPost(User user) {
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpResponse = HttpRequest.post("http://localhost:8090" + "/api/name/post")
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;
    }
}
