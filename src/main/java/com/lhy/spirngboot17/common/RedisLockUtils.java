package com.lhy.spirngboot17.common;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 * @author seaat
 */
@Component
public class RedisLockUtils {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> lockScript;
    private final RedisScript<Long> unlockScript;

    public RedisLockUtils(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.lockScript = new DefaultRedisScript<>(
                getLockLuaScript(),
                Long.class
        );

        this.unlockScript = new DefaultRedisScript<>(
                "if redis.call('hexists', KEYS[1], ARGV[1]) == 1 then " +
                        "if tonumber(redis.call('hget', KEYS[1], ARGV[1])) > 1 then " +
                        "redis.call('hincrby', KEYS[1], ARGV[1], -1); " +
                        "else return redis.call('del', KEYS[1]); " +
                        "end; " +
                        "return 0; " +
                        "end;",
                Long.class
        );
    }

    private static String getLockLuaScript() {
        String luaScript1 = "local expire_time = tonumber(ARGV[2]);" +
                "if redis.call('exists', KEYS[1]) == 0 then " +
                "redis.call('hset', KEYS[1], ARGV[1], 1); " +
                "redis.call('pexpire', KEYS[1], expire_time); " +
                "return 1; " +
                "end; ";
        String luaScript2 = "if redis.call('hexists', KEYS[1], ARGV[1]) == 1 then " +
                "redis.call('hincrby', KEYS[1], ARGV[1], 1); " +
                "redis.call('pexpire', KEYS[1], expire_time); " +
                "return 1; " +
                "end; ";
        String luaScript3 = "return 0;";
        return luaScript1 + luaScript2 + luaScript3;
    }

    public boolean lock(String lockKey, String requestId, long expireTime, TimeUnit timeUnit) {
        long expireTimeMillis = timeUnit.toMillis(expireTime);
        List<String> keys = Collections.singletonList(lockKey);
        Long result = redisTemplate.execute(lockScript, keys, requestId, String.valueOf(expireTimeMillis));
        return result != null && result == 1;
    }

    public boolean unlock(String lockKey, String requestId) {
        List<String> keys = Collections.singletonList(lockKey);
        Long result = redisTemplate.execute(unlockScript, keys, requestId);
        return result != null && result == 1;
    }
}
