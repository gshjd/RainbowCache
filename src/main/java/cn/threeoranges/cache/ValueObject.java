package cn.threeoranges.cache;

import java.io.Serializable;

/**
 * @author: 李小熊
 * @date: 2021/3/10 5:09 下午
 **/
public class ValueObject implements Serializable {
    private static final long serialVersionUID = -3192714094176034587L;
    /**
     * 缓存有效时间(默认：秒)
     */
    private Long expiration;
    /**
     * 缓存值
     */
    private Object value;
    /**
     * 缓存过期时间点
     */
    private Long destroyTime;

    private ValueObject(){}

    ValueObject(Object value, Long expiration, Long destroyTime) {
        this.value = value;
        this.expiration = expiration;
        this.destroyTime = destroyTime;
    }

    public Long getExpiration() {
        return expiration;
    }

    public Object getValue() {
        return value;
    }

    public Long getDestroyTime() {
        return destroyTime;
    }
}
