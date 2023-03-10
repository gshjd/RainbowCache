package cn.threeoranges.aop;

import cn.threeoranges.annotation.RainbowCache;
import cn.threeoranges.annotation.RainbowCacheClear;
import cn.threeoranges.annotation.RainbowCachePut;
import cn.threeoranges.annotation.RainbowDistributedLock;
import cn.threeoranges.cache.Cacheable;
import cn.threeoranges.cache.DistributedLock;
import cn.threeoranges.cache.SimpleCache;
import cn.threeoranges.properties.RainbowCacheProperties;
import cn.threeoranges.properties.enums.RainbowCacheTypeEnum;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cn.threeoranges.properties.enums.RainbowCacheTypeEnum.REDIS;
import static cn.threeoranges.properties.enums.RainbowCacheTypeEnum.SIMPLE;

/**
 * @author xiaoxiong
 */
@Aspect
@Component
public class CacheAspect {
    @Resource
    private RainbowCacheProperties rainbowCacheProperties;
    private RainbowCacheTypeEnum type;
    @Autowired
    private RedisTemplate redisTemplate;
    @Resource
    private Cacheable cacheable;
    @Resource
    private SimpleCache simpleCache;

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
        DistributedLock.distributedLock(redisTemplate, lockKey, rainbowCacheProperties);
        return pjp.proceed();
    }

}

