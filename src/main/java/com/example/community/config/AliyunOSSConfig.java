package com.example.community.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunOSSConfig {
    @Value("${aliyun.endpoint}")
    private String endpoint;

    @Value("${aliyun.sccess-key-id}")
    private String accesaKeyId;

    @Value("${aliyun.sccess-key-secret}")
    private String accesgKeySecret;

    //简单测试一下  配置了一个参数  详细参数可以配置很多
    @Bean
    public ClientBuilderConfiguration configuration(){
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setMaxConnections(200);
        return clientBuilderConfiguration;
    }

    @Bean
    public OSS ossClientBuilder(ClientBuilderConfiguration configuration){
        OSSClientBuilder ossClientBuilder = new OSSClientBuilder();
        OSS build = ossClientBuilder.build(endpoint, accesaKeyId, accesgKeySecret, configuration);
        return build;
    }

}