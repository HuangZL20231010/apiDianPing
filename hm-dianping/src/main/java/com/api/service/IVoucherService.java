package com.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.api.dto.Result;
import com.api.entity.Voucher;


public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
