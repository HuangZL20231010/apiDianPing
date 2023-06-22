package com.api.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.api.exception.NoContentException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.api.dto.Result;
import com.api.entity.Shop;
import com.api.mapper.ShopMapper;
import com.api.service.IShopService;
import com.api.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.api.utils.RedisConstants.*;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) {
        //解决缓存穿透
        Shop shop = queryWithPassThrough(id);

        //互斥锁 解决缓存击穿
//        Shop shop = queryWithMutex(id);

        //利用逻辑过期解决缓存击穿问题
//        Shop shop = queryWithLogicTimeOut(id);

        //返回
        if(shop==null)
            //return Result.fail("店铺不存在");
            throw new NoContentException("店铺不存在");
        return Result.ok(shop);
    }

    /**
     * 利用逻辑过期解决缓存击穿问题
     * @param id
     * @return
     */
    public Shop queryWithLogicTimeOut(Long id){
        //1.直接查询reidis
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //2.判断是否存在
        if(StrUtil.isBlank(shopJson)){
            //不存在，说明该shop没有提前缓存热点key到redis 直接返回
            return null;
        }

        //3.存在 json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject shopData =(JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(shopData, Shop.class);

        //4.判断是否过期
        LocalDateTime expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            //5.没过期，直接返回数据

System.out.println("还没过期");
System.out.println(expireTime);
System.out.println(LocalDateTime.now());
            return shop;
        }
        //6.过期了 缓存重建
        //6.1获取互斥锁
        String key = LOCK_SHOP_KEY+id;
        boolean flag = tryLock(key);
        //6.2判断是否获取锁成功
        if(flag){
            //6.3.1 成功 先进行double check
            shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
            if(StrUtil.isBlank(shopJson)){
                return null;
            }
            redisData = JSONUtil.toBean(shopJson, RedisData.class);
            shopData =(JSONObject) redisData.getData();
            shop = JSONUtil.toBean(shopData, Shop.class);
            expireTime = redisData.getExpireTime();
            if(expireTime.isAfter(LocalDateTime.now())){
                return shop;
            }

            //6.3.2 若依然过期 再开启独立线程 实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //重建缓存
                    this.saveShop2Redis(id,20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    //释放锁
                    unlock(key);
                }
            });
            return shop;
        }

        //6.4 不管有没有拿到锁 都直接返回旧数据
        return shop;
    }

    //互斥方法 解决缓存击穿
    public Shop queryWithMutex(Long id){
        //1.直接查询reidis
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        System.out.println(shopJson);
        //2.存在数据，返回
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //3.可能是 空字符串的情况
        //空 说明为了解决缓存穿透 在redis中设置了空的数据 防止请求多次打到数据库中
        if(shopJson!=null){
            return null;
        }


        String key= null;
        Shop shop = null;
        try {
            //4.redis未命中 不存在数据
            //4.1加锁
            key = LOCK_SHOP_KEY+id;
            if(!tryLock(key)){
                //4.2若没有获取到 休眠
                Thread.sleep(200);
                return queryWithMutex(id);
            }

            //4.3获取到锁了
            //5 再查一次 double check
            shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
            System.out.println(shopJson);
            //存在数据，返回
            if(StrUtil.isNotBlank(shopJson)){
                shop = JSONUtil.toBean(shopJson, Shop.class);
                return shop;
            }

            //redis依然没有数据
            //6.查询数据库
            shop = getById(id);
            //7.不存在，返回商家不存在
            if(shop==null){
                //把不存在的信息也存入redis中
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }

            //7.存在 写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {

            //8.释放锁
            unlock(key);
        }


        //9.返回
        return shop;
    }

    //解决缓存穿透
    public Shop queryWithPassThrough(Long id){
        //1.直接查询reidis
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        System.out.println(shopJson);
        //2.存在数据，返回
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //3.可能是 空字符串的情况
        //空 说明为了解决缓存穿透 在redis中设置了空的数据 防止请求多次打到数据库中
        if(shopJson!=null){
            return null;
        }


        //4.不存在，查数据库
        Shop shop = getById(id);

        //5.不存在，返回商家不存在
        if(shop==null){
            //把不存在的信息也存入redis中
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }

        //6.存在 写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //7.返回
        return shop;
    }

    private boolean tryLock(String key){

        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }


    /**
     * 将商铺信息+逻辑过期时间写入redis
     * @param id
     * @param expireSeconds
     */
    public void saveShop2Redis(Long id,Long expireSeconds){
        //1.查询店铺信息
        Shop shop = getById(id);
        System.out.println("查到数据了："+shop);
        //2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        //3.写入Redis
        String json = JSONUtil.toJsonStr(redisData);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,json);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        if(shop.getId()==null){
            return Result.fail("id不能为空");
        }

        //更新数据库
        updateById(shop);

        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());

        return Result.ok();
    }
}
