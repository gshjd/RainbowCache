package cn.threeoranges.annotation;

import java.lang.annotation.*;

/**
 * @author: 李小熊
 * @date: 2021/3/2 4:53 下午
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RainbowCache {
    /**
     * 缓存键
     */
    String[] keys() default {};

    /**
     * 动态建
     */
    String dynamicKey() default "";

    /**
     * 有效时间
     */
    long expiration() default -1L;

    /**
     * 自动续约
     */
    boolean renew() default false;
}
