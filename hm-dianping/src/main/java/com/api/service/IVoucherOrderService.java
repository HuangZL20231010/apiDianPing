package com.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.api.dto.Result;
import com.api.entity.VoucherOrder;


public interface IVoucherOrderService extends IService<VoucherOrder> {


    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
