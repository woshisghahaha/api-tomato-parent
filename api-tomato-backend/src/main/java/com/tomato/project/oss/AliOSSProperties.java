package com.tomato.project.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
//不能再用@Component了，因为我们不会要求用户手动扫描这个包
//@Component

@ConfigurationProperties(prefix = "aliyun.oss")
public class AliOSSProperties {
    private String endpoint;
    private String accessKeyId    ;
    private String accessKeySecret;
    private String bucketName     ;
}
