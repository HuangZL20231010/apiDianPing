package com.api.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.api.dto.UserDTO;
import com.api.utils.JWTUtils;
import com.api.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.api.utils.RedisConstants.LOGIN_USER_KEY;
import static com.api.utils.RedisConstants.LOGIN_USER_TTL;
@Slf4j
public class JWTandRefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate redisTemplate;

    public JWTandRefreshTokenInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //拦截器取到请求先进行判断，如果是OPTIONS请求，则放行
        if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            System.out.println("Method:OPTIONS");
            return true;
        }

        log.info("被拦截了");

        Map<String,Object> map = new HashMap<>();
        // 获取请求头中令牌
        String token = request.getHeader("Authorization");
        if(StrUtil.isNotBlank(token))token = token.substring(7);
        System.out.println("token: " + token);
        try {
            // 验证令牌
            //先看redis中有没有，如果有，就使用,并延长过期时间
            String key= LOGIN_USER_KEY+token;
            String json = redisTemplate.opsForValue().get(key);
//            Map<Object, Object> userMap = redisTemplate.opsForHash().entries(key);
            UserDTO userDTO;
            if(StrUtil.isNotBlank(json)){
                userDTO = JSONUtil.toBean(json, UserDTO.class);
                redisTemplate.expire(key,LOGIN_USER_TTL, TimeUnit.MINUTES);//一天过期

            }else {
                //如果redis中没有，就走jwt验证，并存到redis中设置过期时间1天
                DecodedJWT verify = JWTUtils.verify(token);
                log.info("用户【"+verify.getClaim("phone").asString()+"】正在访问");
                String id= verify.getClaim("id").asString();
                String type= verify.getClaim("type").asString();
                userDTO = new UserDTO(Long.valueOf(id), " ", " ",Integer.valueOf(type));
                log.debug(userDTO.toString());
                redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(userDTO),LOGIN_USER_TTL,TimeUnit.DAYS);
            }

            UserHolder.saveUser(userDTO);
            return true;  // 放行请求
        } catch (SignatureVerificationException e) {
            e.printStackTrace();
            map.put("msg","无效签名！");
        }catch (TokenExpiredException e){
            e.printStackTrace();
            map.put("msg","token过期");
        }catch (AlgorithmMismatchException e){
            e.printStackTrace();
            map.put("msg","算法不一致");
        }catch (Exception e){
            e.printStackTrace();
            response.setStatus(401);
            map.put("msg","token无效！");
        }
        map.put("state",false);  // 设置状态
        // 将map以json的形式响应到前台  map --> json  (jackson)
        String json = new ObjectMapper().writeValueAsString(map);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().println(json);
        return false;
    }

//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // 1.获取请求头中的token
//        String token = request.getHeader("authorization");
//
//        if (StrUtil.isBlank(token)) {
//            return true;
//        }
//        token = token.substring(7);
//
//        // 2.基于TOKEN获取redis中的用户
//        String key  = LOGIN_USER_KEY + token;
//        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
//        // 3.判断用户是否存在 因为有可能是假token
//        if (userMap.isEmpty()) {
//            return true;
//        }
//        // 5.将查询到的hash数据转为UserDTO
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
//        // 6.存在，保存用户信息到 ThreadLocal
//        UserHolder.saveUser(userDTO);
//        // 7.刷新token有效期
//        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
//        // 8.放行
//        return true;
//
//
//
//    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
