package cn.threeoranges.aop;

import cn.threeoranges.annotation.RainbowCache;
import cn.threeoranges.annotation.RainbowCacheClear;
import cn.threeoranges.annotation.RainbowCachePut;
import cn.threeoranges.annotation.RainbowDistributedLock;
import cn.threeoranges.cache.Cacheable;
import cn.threeoranges.thread.pool.WatchDogThreadPool;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * @Author: huangyifan
 * @Date: 2020/2/16 20:42
 * @Desc: 角色权限aop
 */

@Aspect
public class CacheAspect {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private final Cacheable cacheable = Cacheable.cacheable();
    private final cn.threeoranges.cache.RainbowCache rainbowCache = cn.threeoranges.cache.RainbowCache.getRainbowCache();

    /**
     * 缓存数据
     *
     * @param pjp
     * @param rainbowCache
     * @return
     * @throws Throwable
     */
    @Around("@annotation(rainbowCache)")
    public Object cache(ProceedingJoinPoint pjp, RainbowCache rainbowCache) throws Throwable {
        Object obj = null;

        if (redisTemplate != null) {
            obj = cacheable.redisCache(pjp, rainbowCache);
        }
        obj = cacheable.localCache(obj, pjp, rainbowCache);
        return obj;
    }

    @Before("@annotation(rainbowCacheClear)")
    public void cacheClear(RainbowCacheClear rainbowCacheClear) {
        for (String value : rainbowCacheClear.keys()) {
            String key = value + ":" + rainbowCacheClear.dynamicKey();
            // 处理Redis
            if (redisTemplate != null) {
                Set<String> keys = redisTemplate.keys(key);
                if (keys == null || keys.size() == 0) {
                    continue;
                }
                redisTemplate.delete(keys);
            }
            // 处理本地缓存
            Set<String> keys = rainbowCache.keys(key);
            if (keys == null || keys.size() == 0) {
                continue;
            }
            rainbowCache.delete(keys);
        }
    }

    @Around("@annotation(rainbowCachePut)")
    public Object cachePut(ProceedingJoinPoint pjp, RainbowCachePut rainbowCachePut) throws Throwable {
        Object obj = pjp.proceed();
        long expiration = rainbowCachePut.expiration();

        for (String key : rainbowCachePut.keys()) {
            if (obj != null) {
                // 不带失效时间的缓存
                if (expiration < 0) {
                    rainbowCache.setCache(key, obj);
                    if (redisTemplate != null) {
                        redisTemplate.opsForValue().set(key, obj);
                    }
                    continue;
                }
                // 带有失效时间的缓存
                rainbowCache.setCache(key, obj, expiration, TimeUnit.SECONDS);
                if (redisTemplate != null) {
                    redisTemplate.opsForValue().set(key, obj, expiration, TimeUnit.MILLISECONDS);
                }
            }
        }
        return obj;
    }

    @Around("@annotation(distributedLock)")
    public Object distributedLock(ProceedingJoinPoint pjp, RainbowDistributedLock distributedLock) throws Throwable {
        String lockKey = "rainbowDistributedLock:" + distributedLock.key();
        String lockValue = "lock";
        long lockTime = 5;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockTime, TimeUnit.SECONDS);
        if (success == null) {
            throw new RuntimeException("lock error");
        }

        while (!success) {
            TimeUnit.SECONDS.sleep(1);
            success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockTime, TimeUnit.SECONDS);
            if (success == null) {
                throw new RuntimeException("lock error");
            }
        }

        // 开启看门狗
        watchDog(lockKey, lockValue, lockTime);

        return pjp.proceed();
    }

    /**
     * 看门狗
     *
     * @param lockKey
     * @param lockValue
     * @param lockTime
     */
    public void watchDog(String lockKey, String lockValue, Long lockTime) {
        long threadId = Thread.currentThread().getId();

        // 自动续期
        Runnable runnable = () -> {
            ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
            ThreadInfo info = mxBean.getThreadInfo(threadId);

            while (info != null) {
                redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockTime, TimeUnit.SECONDS);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException("线程异常, 线程id: " + Thread.currentThread().getId() + "{}", e);
                }
                info = mxBean.getThreadInfo(threadId);
            }

            redisTemplate.delete(lockKey);
        };

        WatchDogThreadPool.getInstance().execute(runnable);
    }
}
