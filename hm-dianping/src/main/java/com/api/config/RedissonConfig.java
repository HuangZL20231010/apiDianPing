package com.api.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        //配置类
        Config config = new Config();
        //添加reids的地址，这里添加了单点的地址
        config.useSingleServer().setAddress("redis://8.130.92.141:6379").setPassword("root");
        //创建客户端
        return Redisson.create(config);
    }
}
