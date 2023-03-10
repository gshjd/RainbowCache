package cn.threeoranges.cache;

import cn.threeoranges.properties.RainbowCacheProperties;
import cn.threeoranges.thread.pool.WatchDogThreadPool;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author: 李小熊
 * @date: 2021/5/13 9:42 上午
 **/
public class DistributedLock {

    private static final String LOCK_VALUE = "LOCK";
    private static final long LOCK_TIME = 5;

    /**
     * 获取分布式锁
     *
     * @param redisTemplate
     * @param lockKey
     * @param rainbowCacheProperties
     * @throws Throwable
     */
    public static void distributedLock(RedisTemplate redisTemplate, String lockKey, RainbowCacheProperties rainbowCacheProperties) throws Throwable {


        long startTime = System.currentTimeMillis();
        long timeOut = rainbowCacheProperties.getTimeOut();
        boolean success = getLock(redisTemplate, lockKey);
        while (!success) {
            TimeUnit.MILLISECONDS.sleep(10);
            success = getLock(redisTemplate, lockKey);
            if (timeOut != -1 && System.currentTimeMillis() - startTime > timeOut) {
                throw new TimeoutException("The lock acquisition time is too long for more than " + timeOut + " seconds");
            }
        }

        // 开启看门狗
        watchDog(redisTemplate, lockKey);
    }

    /**
     * 尝试获取锁
     *
     * @param redisTemplate
     * @param lockKey
     * @return
     */
    public static boolean getLock(RedisTemplate redisTemplate, String lockKey) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, LOCK_VALUE, LOCK_TIME, TimeUnit.SECONDS);
        if (success == null) {
            throw new RuntimeException("get lock error");
        }
        return success;
    }

    /**
     * 释放锁
     *
     * @param redisTemplate
     * @param lockKey
     */
    public static void deleteLock(RedisTemplate redisTemplate, String lockKey) {
        redisTemplate.delete(lockKey);
    }

    /**
     * 看门狗
     *
     * @param lockKey lockKey
     */
    private static void watchDog(RedisTemplate redisTemplate, String lockKey) {
        long threadId = Thread.currentThread().getId();

        // 自动续期
        Runnable runnable = () -> {
            ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
            ThreadInfo info = mxBean.getThreadInfo(threadId);

            while (info != null) {
                getLock(redisTemplate, lockKey);
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
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
