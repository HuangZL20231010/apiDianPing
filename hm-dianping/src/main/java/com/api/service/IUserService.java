package com.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.api.dto.LoginFormDTO;
import com.api.dto.Result;
import com.api.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {



    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sedCode(String phone, HttpSession session);

    Result sign();

    Result signCount();
}
