package com.tomato.project.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * 阿里云 OSS 工具类
 */
//不能再用@Component了，因为我们不会要求用户手动扫描这个包
//@Component
@Data
public class AliOSSUtils {

    private AliOSSProperties aliOSSProperties;

    /**
     * 实现上传图片到OSS
     */
    public String uploadFile2OSS(MultipartFile file,String fileName) throws IOException {
        String endpoint         =aliOSSProperties.getEndpoint();
        String accessKeyId      =aliOSSProperties.getAccessKeyId();
        String accessKeySecret  =aliOSSProperties.getAccessKeySecret();
        String bucketName       =aliOSSProperties.getBucketName();

        // 避免文件覆盖,获取上传文件的原始名字并构建新名字，该步骤应在control层进行，因为要根据业务划分目录
        // 获取上传的文件的输入流
        InputStream inputStream = file.getInputStream();

        // 创建上传Object的Metadata
        // metadata是用户自定义的元数据，以键值对的形式存储在OSS中。
        // 元数据可以用于标识文件的用途或其他信息。例如，可以将文件的创建时间、作者、版本号等信息存储在元数据中。
        ObjectMetadata objectMetadata = new ObjectMetadata();
        // ObjectMetadata是Amazon S3存储的对象元数据，包括自定义用户提供的元数据以及Amazon S3发送和接收的标准HTTP头
        // etContentLength()方法设置了该对象的长度
        objectMetadata.setContentLength(inputStream.available());
        // setCacheControl()方法设置了缓存控制，表示不用缓存控制
        objectMetadata.setCacheControl("no-cache");
        objectMetadata.setHeader("Pragma", "no-cache");
        objectMetadata.setContentType(getContentType(fileName.substring(fileName.lastIndexOf("."))));
        // setContentDisposition()方法设置了内容描述
        objectMetadata.setContentDisposition("attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

        //上传文件到 OSS
        OSS ossClient = new OSSClientBuilder().build(endpoint,accessKeyId,accessKeySecret);
        ossClient.putObject(bucketName, fileName, inputStream,objectMetadata);

        //拼接文件访问路径
        //请确保文件名的最前面没有"/"
        String url = URL(fileName);
        //String url = endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + fileName;
        // 关闭ossClient
        ossClient.shutdown();
        return url;// 把上传到oss的路径返回
    }

    /**
     * 提前返回URL，供数据库更新，然后再进行文件上传，这样文件上传失败，数据库可以回滚，而反之，数据库更新失败，文件却不能撤回上传
     * @param fileName 文件名
     * @return URL
     */
    public String URL(String fileName){
        String endpoint         =aliOSSProperties.getEndpoint();
        String bucketName       =aliOSSProperties.getBucketName();

//        return endpoint.split("\\.")[0] + "//" + bucketName + "." + endpoint.split("\\.")[1] + "/" + fileName;
        return "https://" + bucketName + "." + endpoint + "/" + fileName;
    }

    /**
     * Description: 判断OSS服务文件上传时文件的contentType
     *
     * @param filenameExtension 文件后缀
     * @return String
     */
    public static String getContentType(String filenameExtension) {
        if (filenameExtension.equalsIgnoreCase("bmp")) {
            return "image/bmp";
        }
        if (filenameExtension.equalsIgnoreCase("gif")) {
            return "image/gif";
        }
        if (filenameExtension.equalsIgnoreCase("jpeg") || filenameExtension.equalsIgnoreCase("jpg")
                || filenameExtension.equalsIgnoreCase("png")) {
            return "image/jpeg";
        }
        if (filenameExtension.equalsIgnoreCase("html")) {
            return "text/html";
        }
        if (filenameExtension.equalsIgnoreCase("txt")) {
            return "text/plain";
        }
        if (filenameExtension.equalsIgnoreCase("vsd")) {
            return "application/vnd.visio";
        }
        if (filenameExtension.equalsIgnoreCase("pptx") || filenameExtension.equalsIgnoreCase("ppt")) {
            return "application/vnd.ms-powerpoint";
        }
        if (filenameExtension.equalsIgnoreCase("docx") || filenameExtension.equalsIgnoreCase("doc")) {
            return "application/msword";
        }
        if (filenameExtension.equalsIgnoreCase("xml")) {
            return "text/xml";
        }
        // 当浏览器无法确定文件类型时，它将使用application/octet-stream作为默认值。
        return "application/octet-stream";
    }

}
