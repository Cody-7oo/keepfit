-- limit.lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local expire = tonumber(ARGV[2])

-- 自增
local current = redis.call('incr', key)
-- 第一次访问设置过期时间
if current == 1 then
    redis.call('expire', key, expire)
end

-- 是否超过阈值
if current > limit then
    return 0
end
return 1