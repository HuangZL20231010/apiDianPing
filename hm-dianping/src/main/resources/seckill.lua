-- 1.参数列表
-- 1.1优惠券id
local voucherId = ARGV[1]
-- 1.2用户id
local userId = ARGV[2]

-- 2.数据key
-- 2.1库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2订单key
local orderKey = 'seckill:stock:' .. voucherId

-- 3.脚本业务
-- 3.1.判断库存是否充足
if(tonumber(redis.call('get',stockKey))<0)then
    return -1
end
-- 3.2判断用户是否下过单
if(redis.call('sismember',orderKey,userId)==1)then
    return 2
end
-- 3.4.扣库存
redis.call('incrby',stockKey,-1)
-- 3.5.下单（保存用户）
redis.call('sadd',orderKey.userId)
