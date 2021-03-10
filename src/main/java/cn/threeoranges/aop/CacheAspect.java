package cn.threeoranges.aop;

import cn.threeoranges.annotation.RainbowCache;
import cn.threeoranges.annotation.RainbowCacheClear;
import cn.threeoranges.annotation.RainbowCachePut;
import cn.threeoranges.annotation.RainbowDistributedLock;
import cn.threeoranges.cache.Cacheable;
import cn.threeoranges.cache.SimpleCache;
import cn.threeoranges.properties.RainbowCacheProperties;
import cn.threeoranges.properties.enums.RainbowCacheTypeEnum;
import cn.threeoranges.thread.pool.WatchDogThreadPool;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static cn.threeoranges.properties.enums.RainbowCacheTypeEnum.REDIS;
import static cn.threeoranges.properties.enums.RainbowCacheTypeEnum.SIMPLE;

/**
 * @author xiaoxiong
 */
@Aspect
public class CacheAspect {
    @Resource
    private RainbowCacheProperties rainbowCacheProperties;
    private RainbowCacheTypeEnum type;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private final Cacheable cacheable = Cacheable.cacheable();
    private final SimpleCache simpleCache = SimpleCache.simpleCache();

    /**
     * 缓存数据
     *
     * @param pjp          pjp
     * @param rainbowCache rainbowCache
     * @return result
     * @throws Throwable throwable
     */
    @Around("@annotation(rainbowCache)")
    public Object cache(ProceedingJoinPoint pjp, RainbowCache rainbowCache) throws Throwable {
        Object obj = null;
        type = rainbowCacheProperties.getType();

        // 配置文件中选择本地缓存 或者 redis无法连接
        // 使用本地缓存
        if (SIMPLE.equals(type) || redisTemplate == null) {
            obj = cacheable.localCache(pjp, rainbowCache);
        }

        // 使用redis缓存
        if (REDIS.equals(type) && redisTemplate != null) {
            obj = cacheable.redisCache(pjp, rainbowCache, redisTemplate);
        }
        return obj;
    }

    /**
     * 清除缓存
     *
     * @param rainbowCacheClear rainbowCacheClear
     */
    @Around("@annotation(rainbowCacheClear)")
    public Object cacheClear(ProceedingJoinPoint pjp, RainbowCacheClear rainbowCacheClear) throws Throwable {
        String dynamicKey = Cacheable.getValue(pjp, rainbowCacheClear.dynamicKey());
        for (String value : rainbowCacheClear.keys()) {
            if (!"".equals(dynamicKey)) {
                value += ":" + dynamicKey;
            }
            // 处理Redis
            if (REDIS.equals(type) || redisTemplate != null) {
                Set<String> keys = redisTemplate.keys(value);
                if (keys == null || keys.size() == 0) {
                    continue;
                }
                redisTemplate.delete(keys);
            }
            // 处理本地缓存
            Set<String> keys = simpleCache.keys(value);
            if (keys == null || keys.size() == 0) {
                continue;
            }
            simpleCache.delete(keys);
        }
        return pjp.proceed();
    }

    /**
     * 放置缓存
     *
     * @param pjp             pjp
     * @param rainbowCachePut rainbowCachePut
     * @return object
     * @throws Throwable throwable
     */
    @Around("@annotation(rainbowCachePut)")
    public Object cachePut(ProceedingJoinPoint pjp, RainbowCachePut rainbowCachePut) throws Throwable {
        Object obj = pjp.proceed();
        long expiration = rainbowCachePut.expiration();

        for (String key : rainbowCachePut.keys()) {
            if (obj != null) {
                // 不带失效时间的缓存
                if (expiration < 0) {
                    simpleCache.setCache(key, obj);
                    if (redisTemplate != null) {
                        redisTemplate.opsForValue().set(key, obj);
                    }
                    continue;
                }
                // 带有失效时间的缓存
                simpleCache.setCache(key, obj, expiration, TimeUnit.SECONDS);
                if (redisTemplate != null) {
                    redisTemplate.opsForValue().set(key, obj, expiration, TimeUnit.MILLISECONDS);
                }
            }
        }
        return obj;
    }

    /**
     * 分布式锁
     *
     * @param pjp
     * @param distributedLock
     * @return
     * @throws Throwable
     */
    @Around("@annotation(distributedLock)")
    public Object distributedLock(ProceedingJoinPoint pjp, RainbowDistributedLock distributedLock) throws Throwable {
        String lockKey = "rainbowDistributedLock:" + distributedLock.key();
        String lockValue = "lock";
        long lockTime = 5;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockTime, TimeUnit.SECONDS);
        if (success == null) {
            throw new RuntimeException("lock error");
        }

        long startTime = System.currentTimeMillis();
        long timeOut = rainbowCacheProperties.getTimeOut();

        while (!success) {
            TimeUnit.SECONDS.sleep(1);
            success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockTime, TimeUnit.SECONDS);
            if (success == null) {
                throw new RuntimeException("lock error");
            }
            if (timeOut != -1 && System.currentTimeMillis() - startTime > timeOut) {
                throw new TimeoutException("The lock acquisition time is too long for more than " + timeOut + " seconds");
            }
        }

        // 开启看门狗
        watchDog(lockKey, lockValue, lockTime);

        return pjp.proceed();
    }

    /**
     * 看门狗
     *
     * @param lockKey   lockKey
     * @param lockValue lockValue
     * @param lockTime  lockTime
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

