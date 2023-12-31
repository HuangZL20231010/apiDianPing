package com.api.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.api.exception.BadRequestException;
import com.api.exception.UnauthorizedException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.api.dto.LoginFormDTO;
import com.api.dto.Result;
import com.api.entity.User;
import com.api.mapper.UserMapper;
import com.api.service.IUserService;
import com.api.utils.JWTUtils;
import com.api.utils.RegexUtils;
import com.api.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.api.utils.RedisConstants.*;
import static com.api.utils.RedisConstants.USER_SIGN_KEY;
import static com.api.utils.SystemConstants.USER_NICK_NAME_PREFIX;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sedCode(String phone, HttpSession session) {
        //1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合，抛出异常
            throw new BadRequestException("手机号格式错误");
        }

        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4. 保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5. 发送验证码
        log.debug("发送短信验证码成功，验证码:{}",code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1. 校验手机号
        // 不需要了
        String phone = loginForm.getPhone();
//        if (RegexUtils.isPhoneInvalid(phone)) {
//            return Result.fail("手机号格式错误");
//        }


        //2. 校验验证码
//        Object cacheCode = session.getAttribute("code");
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)){
            //3. 不一致，报错
//            return Result.fail("验证码错误");
            throw new UnauthorizedException("验证码错误");
        }

        //4.一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();

        //5. 判断用户是否存在
        if (user == null){
            //6. 不存在，创建新用户
            user = createUserWithPhone(phone);
        }

        //7.保存用户信息到redis
        //7.1 随机生成token，作为登录令牌
//        String token = UUID.randomUUID().toString(true);
        Map<String,String> payload=new HashMap<>();
        payload.put("id",String.valueOf(user.getId()));
        payload.put("phone",user.getPhone());
        payload.put("type", String.valueOf(user.getType()));
        String token = JWTUtils.getToken(payload);

        //7.2 将User对象转为HashMap存储
//        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
//                CopyOptions.create()
//                        .setIgnoreNullValue(true)
//                        .setFieldValueEditor((filedName, fieldValue) -> fieldValue.toString()));


        //7.3 存储
//        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);

        //7.4 设置token的有效期
//        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);

        //8.返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }


    @Override
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }



    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        user.setType(0);
        // 2.保存用户
        save(user);
        return user;
    }
}
