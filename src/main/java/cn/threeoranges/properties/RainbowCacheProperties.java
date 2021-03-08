package cn.threeoranges.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author: 李小熊
 * @date: 2021/3/8 1:34 下午
 **/

@Configuration
@ConfigurationProperties(prefix = "rainbow.cache")
public class RainbowCacheProperties {
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
