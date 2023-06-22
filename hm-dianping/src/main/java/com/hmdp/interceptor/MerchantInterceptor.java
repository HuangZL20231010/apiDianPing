package com.hmdp.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.hmdp.utils.SystemConstants.USER_TYPE_MERCHANT;

@Slf4j
public class MerchantInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("被拦截了 在验证管理员的身份 ");

        Integer type = UserHolder.getUser().getType();
        System.out.println(type);
        if(!Objects.equals(type, USER_TYPE_MERCHANT)){
            Map<String,Object> map = new HashMap<>();
            map.put("state",false);  // 设置状态
            map.put("data","您没有权限哦~可联系管理员申请权限（20231010@bjtu.edu.cn）");
            String json = new ObjectMapper().writeValueAsString(map);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println(json);
            response.setStatus(401);
            return false;
        }

        return true;


//        //判断是否需要拦截
//        if(UserHolder.getUser()==null){
//            // 不存在，拦截
//            response.setStatus(401);
//            //401表示 告诉前端 先登录
//            return false;
//        }
//
//        //8. 有用户、放行
//        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
