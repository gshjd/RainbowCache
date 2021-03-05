package cn.threeoranges.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author: 李小熊
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RainbowCachePut {
    /**
     * 缓存键
     */
    String[] keys() default {};

    /**
     * 动态建
     *
     * @return String
     */
    String dynamicKey() default "";

    /**
     * 有效时间
     *
     * @return long
     */
    long expiration() default -1L;

    /**
     * 自动续约
     *
     * @return boolean
     */
    boolean renew() default false;
}
