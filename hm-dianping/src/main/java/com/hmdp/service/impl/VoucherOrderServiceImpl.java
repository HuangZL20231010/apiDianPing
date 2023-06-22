package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    ISeckillVoucherService seckillVoucherService;

    @Resource
    RedisIdWorker redisIdWorker;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    private BlockingQueue<VoucherOrder> orderTasks=new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR= Executors.newSingleThreadExecutor();

    //类初始化的时候执行线程池
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{

        @Override
        public void run() {
            while (true){
                try {
                    //1.获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    //2.创建订单
                    handleCoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                }

            }
        }

        private void handleCoucherOrder(VoucherOrder voucherOrder) {
            //获取用户
            Long userId = voucherOrder.getUserId();

            /**
             * redis实现分布式锁
             */
            //获取锁对象
            RLock lock = redissonClient.getLock("lock:order:" + userId);//redisson自带的可重入锁

            //获取锁
            boolean success = lock.tryLock();
            //获取锁失败
            if(!success) {
                log.error("不能重复下单");
                return;
            }
            //获取锁成功
            try {
                //获取代理对象（事务）
                proxy.createVoucherOrder(voucherOrder);
            }finally {
                //手动释放锁
                lock.unlock();
            }
        }
    }

    //lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private IVoucherOrderService proxy;

    @Override
    public Result seckillVoucher(Long voucherId) {
      //优化秒杀
        Long userId = UserHolder.getUser().getId();
        //1.执行lua脚本
        Long result = redisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId, userId);

        //2.查询结果是否为0
        int r = result.intValue();
        if(r!=0){
            //2.1不为0 没有购买资格
            return Result.fail(r==1?"库存不足":"不能重复下单");
        }
        //2.2为0 有购买资格 并把下单信息保存到阻塞队列
        //2.3创建order
        VoucherOrder voucherOrder = new VoucherOrder();
        //2.4代金券id
        voucherOrder.setVoucherId(voucherId);
        //2.5订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //2.6用户id
        voucherOrder.setUserId(userId);

        //3.获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        //4.返回订单id
        return Result.ok(orderId);
    }
//
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //1.查询优惠券信息
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//
//        //2.判断是否到达秒杀时间
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//
//        //3.判断是否结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//
//        //4.判断库存是否充足
//        if(voucher.getStock()<1){
//            return Result.fail("来晚了，已经被抢光了！");
//        }
//
//        Long userId = UserHolder.getUser().getId();
//
//        /**
//         * redis实现分布式锁
//         */
//        //获取锁对象
////        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, redisTemplate);
//        RLock lock = redissonClient.getLock("lock:order:" + userId);//redisson自带的可重入锁
//
//        //获取锁
////        boolean success = lock.tryLock(10);
//        boolean success = lock.tryLock();
//        //获取锁失败
//        if(!success)return Result.fail("不能重复下单");
//        //获取锁成功
//        try {
//            //获取代理对象（事务）
//            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }finally {
//            //手动释放锁
//            lock.unlock();
//        }
//
//    }

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //5一人一单
        Long userId = voucherOrder.getUserId();
        //5.1 查询是否已经下单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        //5.2 判断数量是否大于0
        if(count>0) {
            log.error("抢太多了，给别人留点吧");
            return ;
        }

        //5.扣库存
        boolean success = seckillVoucherService.update().setSql("stock=stock-1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock",0)//乐观锁   CAS
                .update();
        if(!success) {
            log.error("库存不足！！！！！！");
            return ;
        }

        //6.4存到数据库
        save(voucherOrder);

    }
}
