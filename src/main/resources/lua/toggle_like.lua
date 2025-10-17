-- KEYS[1]=likedKey, KEYS[2]=countKey, KEYS[3]=pendingSet, KEYS[4]=deltaSetKey
-- ARGV[1]=userId, ARGV[2]=picId
local likedKey = KEYS[1]
local countKey = KEYS[2]
local pendingSet = KEYS[3]
local deltaSetKey = KEYS[4]
local userId = ARGV[1]
local picId = ARGV[2]

if redis.call('EXISTS', likedKey) == 0 then
  redis.call('SET', likedKey, '1')
  local c = redis.call('INCR', countKey)
  redis.call('SADD', pendingSet, userId)
  redis.call('SADD', deltaSetKey, picId)
  return {1, c}
else
  redis.call('DEL', likedKey)
  local c = redis.call('DECR', countKey)
  if c < 0 then
    redis.call('SET', countKey, 0)
    c = 0
  end
  redis.call('SADD', pendingSet, userId)
  redis.call('SADD', deltaSetKey, picId)
  return {0, c}
end