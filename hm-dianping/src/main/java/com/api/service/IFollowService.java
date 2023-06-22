package com.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.api.dto.Result;
import com.api.entity.Follow;


public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
