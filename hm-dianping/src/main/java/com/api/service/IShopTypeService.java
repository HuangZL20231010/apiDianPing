package com.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.api.dto.Result;
import com.api.entity.ShopType;

public interface IShopTypeService extends IService<ShopType> {

    Result queryList();
}
