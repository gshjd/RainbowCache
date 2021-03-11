package cn.threeoranges.window.tiny.lru;

import cn.threeoranges.properties.RainbowCacheProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author: 李小熊
 * @date: 2021/3/11 2:31 下午
 **/
@Configuration
public class WindowTinyLru {
    @Resource
    private RainbowCacheProperties rainbowCacheProperties;
    /**
     * 冷数据段
     */
    private final Map<String, CacheFrequency> coldDataPassage;
    /**
     * 热数据段
     */
    private final Map<String, CacheFrequency> heatDataPassage;
    /**
     * 冷数据段长度
     */
    private final int coldDataPassageSize;
    /**
     * 热数据段长度
     */
    private final int heatDataPassageSize;

    private WindowTinyLru() {
        // 初始化数据段
        int size = rainbowCacheProperties.getLength() * 2 / 100;
        coldDataPassageSize = size == 0 ? 1 : size;
        heatDataPassageSize = rainbowCacheProperties.getLength() - coldDataPassageSize;
        coldDataPassage = new LinkedHashMap<>(coldDataPassageSize);
        heatDataPassage = new LinkedHashMap<>(heatDataPassageSize);
    }

    public WindowTinyLru windowTinyLru() {
        return Instance.INSTANCE;
    }

    public synchronized void filter(String key) {
        CacheFrequency cacheFrequency = new CacheFrequency(key, 1, System.currentTimeMillis());
        // 队列均为空，数据放入冷段
        if (coldDataPassage.size() == 0 && heatDataPassage.size() == 0) {
            coldDataPassage.put(key, cacheFrequency);
            return;
        }

        // 冷段中查找数据
        // 数据存在，和热数据尾部比较
        CacheFrequency data = coldDataPassage.get(key);
        if (data != null) {
            cacheFrequency.setTotal(cacheFrequency.getTotal() + 1);
            if (heatDataPassage.size() == 0 || heatDataPassage.size() < heatDataPassageSize) {
                // 热数据段未满
                coldDataPassage.remove(key);
                heatDataPassage.put(key, cacheFrequency);
            } else {
                coldDataUpgrade(key, cacheFrequency);
            }
            return;
        }

        // 热数据段中查找数据
        // 数据存在，推到尾部
        data = heatDataPassage.get(key);
        if (data != null) {
            heatDataPassage.put(key, cacheFrequency);
            return;
        }

        // 数据不存在
        coldDataPassage.put(key, cacheFrequency);
        // 淘汰冷数据头部数据
        if (coldDataPassage.size() > coldDataPassageSize) {
            coldDataPassage.remove(coldDataPassage.entrySet().iterator().next().getKey());
        }
    }

    /**
     * 冷数据升级到热数据段
     *
     * @param key
     * @param cacheFrequency
     */
    private void coldDataUpgrade(String key, CacheFrequency cacheFrequency) {
        // 热数据段已满
        Entry<String, CacheFrequency> heatData = heatDataPassage.entrySet().iterator().next();
        // 每小时数据被访问多少次
        long time = (System.currentTimeMillis() - cacheFrequency.getLastTimestamp()) / 3600000;
        time = time == 0 ? 1 : time;
        long count = cacheFrequency.getTotal() / time;
        long heatCount = heatData.getValue().getTotal() / time;

        // 新来数据是否热度大于热数据段头部数据
        if (count >= heatCount) {
            // 加入热数据，淘汰热数据段头部数据到冷数据段
            heatDataPassage.put(key, cacheFrequency);
            coldDataPassage.put(heatData.getKey(), heatData.getValue());
            heatDataPassage.remove(heatData.getKey());
        } else {
            // 推到冷数据段尾部继续积累热度
            coldDataPassage.remove(key);
            coldDataPassage.put(key, cacheFrequency);
        }

        // 冷数据段已满则淘汰头部数据
        if (coldDataPassage.size() >= coldDataPassageSize) {
            coldDataPassage.remove(heatDataPassage.entrySet().iterator().next().getKey());
        }
    }

    private static class Instance {
        private static final WindowTinyLru INSTANCE = new WindowTinyLru();
    }

    public static void main(String[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            map.put(String.valueOf(i), i);
        }

        System.out.println(map.entrySet().iterator().next());

        System.out.println(1000 * 60 *60);
    }
}
