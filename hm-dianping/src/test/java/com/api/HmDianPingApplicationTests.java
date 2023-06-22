package com.api;

import com.api.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    ShopServiceImpl shopService;

    /**
     * 数据预热
     */
    @Test
    public void testSaveShop(){
        shopService.saveShop2Redis(1L,10L);
    }

}
