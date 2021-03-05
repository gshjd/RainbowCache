package cn.threeoranges.annotation;

import java.lang.annotation.*;

/**
 * 分布式锁
 *
 * @author: 李小熊
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RainbowDistributedLock {
    String key() default "";
}
