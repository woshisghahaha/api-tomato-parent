package com.tomato.apitomatointerface.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.tomato.apitomatoclientsdk.model.User;

import java.util.HashMap;
import java.util.Map;

import static com.tomato.apitomatoclientsdk.utils.SignUtils.getSign;

public class testmain {
    public static void main(String[] args) {
        String user = "{\n" +
                "    \"username\": \"nero\"\n" +
                "}";
        Gson gson = new Gson();
        User user1 = gson.fromJson(user, User.class);

        String json = JSONUtil.toJsonStr(user1);
        HttpResponse httpResponse = HttpRequest.post("http://localhost:8123" + "/api/name/user")
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
    }
    private static Map<String, String> getHeaderMap(String body) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", "xxxxxxxxxxxxxxxxxxx");
        // 一定不能直接发送
//        hashMap.put("secretKey", secretKey);
        hashMap.put("nonce", RandomUtil.randomNumbers(4));
        hashMap.put("body", body);
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        hashMap.put("sign", getSign(body, "xxxxxxxxxxxxxxxxxxxxx"));
        return hashMap;
    }
}
