package com.api.controller;


import cn.hutool.core.bean.BeanUtil;
import com.api.dto.LoginFormDTO;
import com.api.dto.Result;
import com.api.dto.UserDTO;
import com.api.entity.User;
import com.api.entity.UserInfo;
import com.api.exception.BadRequestException;
import com.api.exception.NoContentException;
import com.api.exception.UnauthorizedException;
import com.api.service.IUserInfoService;
import com.api.service.IUserService;
import com.api.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;


@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

//    @ExceptionHandler(BadRequestException.class)
//    public ResponseEntity<Result> handleBadRequestException(BadRequestException ex) {
//        return new ResponseEntity<>(Result.fail(ex.getMessage()), HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(NoContentException.class)
//    public ResponseEntity<Result> handleNoContentException(NoContentException ex) {
//        return new ResponseEntity<>(Result.fail(ex.getMessage()), HttpStatus.NO_CONTENT);
//    }
//
//    @ExceptionHandler(UnauthorizedException.class)
//    public ResponseEntity<Result> handleUnauthorizedException(UnauthorizedException ex) {
//        return new ResponseEntity<>(Result.fail(ex.getMessage()), HttpStatus.UNAUTHORIZED);
//    }

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public ResponseEntity<Result> sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return new ResponseEntity<>(userService.sedCode(phone, session), HttpStatus.OK);

    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("login")
    public ResponseEntity<Result> login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return new ResponseEntity<>(userService.login(loginForm, session), HttpStatus.OK);

    }

    /**
     * 检查当前用户是否登录
     * @return
     */
    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 查询用户详情
     * @param userId
     * @return
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 查询用户信息
     * @param userId
     * @return
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }


    /**
     *签到
     * @return
     */
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    /**
     * 获取当月签到天数
     * @return
     */
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
