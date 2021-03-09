package cn.threeoranges.cache;

import cn.threeoranges.annotation.RainbowCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.concurrent.TimeUnit;

/**
 * @author: 李小熊
 **/
public class Cacheable {
    private final cn.threeoranges.cache.RainbowCache rainbowCache = cn.threeoranges.cache.RainbowCache.getRainbowCache();

    private Cacheable() {
    }

    public static Cacheable cacheable() {
        return Instance.INSTANCE;
    }

    /**
     * 使用本地缓存处理
     *
     * @param pjp          pjp
     * @param rainbowCache rainbowCache
     * @return result
     * @throws Throwable throwable
     */
    public Object localCache(ProceedingJoinPoint pjp, RainbowCache rainbowCache) throws Throwable {
        Object object = null;
        // 获取el的值
        String dynamicKey = getValue(pjp, rainbowCache.dynamicKey());

        for (String key : rainbowCache.keys()) {
            // 真正存放缓存的key
            if (!"".equals(dynamicKey)) {
                key += ":" + dynamicKey;
            }
            // 查询key缓存是否存在
            object = this.rainbowCache.getCache(key);
            // 缓存时间
            long expiration = rainbowCache.expiration();
            // 不存在走业务流程并设置缓存
            if (object == null) {
                // 业务返回值
                object = pjp.proceed();
                if (expiration < 0) {
                    this.rainbowCache.setCache(key, object);
                    continue;
                }
                this.rainbowCache.setCache(key, object, expiration, TimeUnit.SECONDS);
                continue;
            }

            // 缓存存在 且 需要续期
            if (rainbowCache.renew()) {
                if (expiration < 0) {
                    this.rainbowCache.setCache(key, object);
                    continue;
                }
                this.rainbowCache.setCache(key, object, expiration, TimeUnit.SECONDS);
            }
        }
        return object;
    }

    /**
     * 使用redis处理缓存
     *
     * @param pjp          pjp
     * @param rainbowCache rainbowCache
     * @return result
     * @throws Throwable throwable
     */
    public Object redisCache(ProceedingJoinPoint pjp, RainbowCache rainbowCache, RedisTemplate<String, Object> redisTemplate) throws Throwable {
        Object object = null;
        // 获取el的值
        String dynamicKey = getValue(pjp, rainbowCache.dynamicKey());
        for (String key : rainbowCache.keys()) {
            // 真正存放缓存的key
            if (!"".equals(dynamicKey)) {
                key += ":" + dynamicKey;
            }

            // 查询key缓存是否存在
            Object result = redisTemplate.opsForValue().get(key);
            // 缓存时间
            long expiration = rainbowCache.expiration();
            // 不存在走业务流程并设置缓存
            if (result == null) {
                // 业务返回值
                object = pjp.proceed();
                if (expiration < 0) {
                    redisTemplate.opsForValue().set(key, object);
                    continue;
                }
                redisTemplate.opsForValue().set(key, object, expiration, TimeUnit.MILLISECONDS);
                continue;
            }

            // 缓存存在 且 需要续期
            if (rainbowCache.renew()) {
                if (expiration < 0) {
                    redisTemplate.opsForValue().set(key, result);
                    continue;
                }
                redisTemplate.opsForValue().set(key, result, expiration, TimeUnit.MILLISECONDS);
            }

            // 记录返回值
            if (object == null) {
                object = result;
            }
        }
        return object;
    }

    /**
     * el表达式获取值
     *
     * @param pjp pjp
     * @param el  el expression
     * @return el result
     */
    public static String getValue(ProceedingJoinPoint pjp, String el) {
        if (el == null || "".equals(el)) {
            return "";
        }
        SpelExpressionParser parserSpEl = new SpelExpressionParser();
        // 解析el表达式
        Expression expression = parserSpEl.parseExpression(el);
        // 获取参数名
        String[] paramNames = ((MethodSignature) pjp.getSignature()).getParameterNames();
        // 获取参数值
        Object[] args = pjp.getArgs();
        // 组装context
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        // 获取表达式值
        Object object = expression.getValue(context);
        return object == null ? "" : object.toString();
    }

    private static class Instance {
        private static final Cacheable INSTANCE = new Cacheable();
    }
}
