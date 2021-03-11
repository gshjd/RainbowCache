package cn.threeoranges.window.tiny.lru;

/**
 * 缓存访问频率
 *
 * @author: 李小熊
 * @date: 2021/3/11 2:33 下午
 **/
public class CacheFrequency {
    private Integer total;
    private Long lastTimestamp;

    CacheFrequency(String key, Integer total, Long lastTimestamp) {
        this.total = total;
        this.lastTimestamp = lastTimestamp;
    }

    public Long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(Long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
