package cn.threeoranges.annotation;

import java.lang.annotation.*;

/**
 * @author: 李小熊
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RainbowCacheClear {
    /**
     * 缓存键
     */
    String[] keys() default {};

    /**
     * 动态建
     */
    String dynamicKey() default "";
}
