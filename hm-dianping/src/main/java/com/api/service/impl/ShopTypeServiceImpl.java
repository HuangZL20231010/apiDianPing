package com.api.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.api.dto.Result;
import com.api.entity.ShopType;
import com.api.mapper.ShopTypeMapper;
import com.api.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    //        List<ShopType> typeList = typeService
//                .query().orderByAsc("sort").list();
//        return Result.ok(typeList);

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {

System.out.println("正在执行query\n\n");
        //1.从redis中查询列表是否存在
        List<String> list = stringRedisTemplate.opsForList().range("shop:type", 0, -1);
System.out.println("list:"+list+"\n\n");

        //2.若存在，直接返回list
        if(!list.isEmpty()){
            //要将string转化成实体类
            ArrayList<ShopType> shopTypeList = new ArrayList<>();
            for (String s : list) {
                shopTypeList.add(JSONUtil.toBean(s, ShopType.class));
            }
System.out.println("shopTypeList (from redis)："+shopTypeList+"\n\n");
            return Result.ok(shopTypeList);

        }

        //3. 若不存在，查询数据库表

        List<ShopType> shopTypeList = query().orderByAsc("sort").list();

System.out.println("shopTypeList："+shopTypeList+"\n\n");
        //4.查询为空 报错
        if(shopTypeList.isEmpty()){
            return Result.fail("查询列表失败");
        }


        //5.将查询结果放到redis中

        // 这里没有转化好 应该把每条数据转成json格式
        for (int i = 0; i < shopTypeList.size(); i++) {
            String s = JSONUtil.toJsonStr(shopTypeList.get(i));
            stringRedisTemplate.opsForList().rightPush("shop:type",s);
        }

        //设置ttl
        stringRedisTemplate.expire("shop:type",30, TimeUnit.MINUTES);


        //6.返回list
        return Result.ok(shopTypeList);
    }
}
