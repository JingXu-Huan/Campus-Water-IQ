---@diagnostic disable: undefined-global
local hash = KEYS[1]
local value = ARGV[1]
local fields = redis.call('HKEYS', hash)
for i = 1, #fields do
    redis.call('HSET', hash, fields[i], value)
end
return true
