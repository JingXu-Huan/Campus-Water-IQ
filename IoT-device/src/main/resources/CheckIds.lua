---@diagnostic disable: undefined-global
for i = 1, #ARGV do
    if redis.call('SISMEMBER', KEYS[1], ARGV[i]) == 0 then
        return 0
    end
end
return 1
