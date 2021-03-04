package cn.threeoranges.annotation;

import java.lang.annotation.*;

/**
 * 分布式锁
 *
 * @author: 李小熊
 * @date: 2021/3/4 2:47 下午
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RainbowDistributedLock {
    String key() default "";
}
