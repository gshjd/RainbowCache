package cn.threeoranges.properties;

import cn.threeoranges.properties.enums.RainbowCacheTypeEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author: 李小熊
 * @date: 2021/3/8 1:34 下午
 **/

@Configuration
@ConfigurationProperties(prefix = "rainbow.cache")
public class RainbowCacheProperties {
    private RainbowCacheTypeEnum type;
    private Long timeOut;
    private Integer length;

    public RainbowCacheProperties() {
        this.type = RainbowCacheTypeEnum.SIMPLE;
        this.timeOut = 30000L;
        this.length = Integer.MAX_VALUE;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public RainbowCacheTypeEnum getType() {
        return type;
    }

    public void setType(RainbowCacheTypeEnum type) {
        this.type = type;
    }

    public Long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(Long timeOut) {
        this.timeOut = timeOut;
    }
}
