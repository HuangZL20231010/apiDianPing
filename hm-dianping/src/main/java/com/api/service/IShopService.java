package com.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.api.dto.Result;
import com.api.entity.Shop;


public interface IShopService extends IService<Shop> {


    Result queryById(Long id);

    Result update(Shop shop);
}
