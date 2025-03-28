local key = KEYS[1] -- 锁的key
local threadId = ARGV[1] -- 线程唯一标识
local releaseTime = ARGV[2] -- 锁的自动释放时间

-- 判断锁是否存在
if (redis.call('exists', key) == 0) then
    -- 锁不存在，获取锁
    redis.call('hset', key, threadId, 1)
    -- 刷新有效期
    redis.call('expire', key, releaseTime)
    return 1 -- 返回结果
end

-- 锁已经存在，判断threadId是否是自己
if (redis.call('hexists', key, threadId) == 1) then
    -- threadId是自己，获取锁，重入次数+1
    redis.call('hincrby', key, threadId, 1)
    -- 刷新有效期
    redis.call('expire', key, releaseTime)
    return 1 -- 返回结果
end
return 0 -- 代码执行到这里说明锁不是自己的，获取锁失败