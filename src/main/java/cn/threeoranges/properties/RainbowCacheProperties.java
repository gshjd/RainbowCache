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
    private long timeOut;

    public RainbowCacheProperties() {
        this.type = RainbowCacheTypeEnum.SIMPLE;
        this.timeOut = 30000;
    }

    public RainbowCacheTypeEnum getType() {
        return type;
    }

    public void setType(RainbowCacheTypeEnum type) {
        this.type = type;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }
}
