package com.hmdp.config;

import com.hmdp.interceptor.MerchantInterceptor;
import com.hmdp.interceptor.JWTandRefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //token刷新拦截器
        registry.addInterceptor(new JWTandRefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns(
                        "/**"
                ).excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/blog/hot"
                )
                .order(0);

        // 登录拦截器
        registry.addInterceptor(new MerchantInterceptor())
                .addPathPatterns(
                        "/shop/add",
                        "/shop/update",
                        "/voucher/add-normal",
                        "/voucher/add-seckill"
                ).order(1);


    }


}
