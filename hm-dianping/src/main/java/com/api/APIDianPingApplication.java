package com.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.api.mapper")
@SpringBootApplication
public class APIDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(APIDianPingApplication.class, args);
    }

}
