local key = KEYS[1] -- 锁的key
local threadId = ARGV[1] -- 线程唯一标识
local releaseTime = ARGV[2] -- 锁的自动释放时间
-- 判断当前锁是否还被自己持有
if (redis.call('hexists', key, threadId) == 0) then
    return nil -- 如果已经不是自己，直接返回
end
-- 是自己的锁，则重入次数-1
local count = redis.call('hincrby', key, threadId, -1)
-- 判断冲入次数是否为0
if (count > 0) then
    -- 大于0说明不能释放锁，重置有效期后返回
    redis.call('expire', key, releaseTime)
    return nil
end
-- 等于0说明可以释放锁
redis.call('del', key)
return nil