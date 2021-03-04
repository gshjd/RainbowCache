package cn.threeoranges.cache;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * @author: 李小熊
 * @date: 2021/3/2 5:00 下午
 **/
public class RainbowCache {

    /**
     * 本地缓存
     */
    private final Map<String, Map<String, Object>> caches = new HashMap<>();
    /**
     * 缓存有效时间(默认：秒)
     */
    private final String EXPIRATION = "expiration";
    /**
     * 缓存值
     */
    private final String VALUE = "value";
    /**
     * 缓存创建时间
     */
    private final String CREATE_TIME = "createTime";
    /**
     * 缓存过期时间点
     */
    private final String DESTROY_TIME = "destroyTime";
    /**
     * 定时清理锁
     */
    private boolean clearLock = false;

    private RainbowCache() {

    }

    /**
     * 定时清理过期缓存(10s)
     */
    @Scheduled(cron = "0/10 * * * * ? *")
    private void cleanUpTask() {
        if (clearLock) {
            return;
        }
        caches.keySet().parallelStream().forEach(this::triggerCleanUp);
        clearLock = false;
    }

    /**
     * 触发清理缓存
     */
    public void triggerCleanUp(String key) {
        long destroyTime = Long.parseLong(caches.get(key).get(DESTROY_TIME).toString());
        if (destroyTime <= System.currentTimeMillis()) {
            caches.remove(key);
        }
    }

    public static RainbowCache getRainbowCache() {
        return Instance.INSTANCE;
    }

    private static class Instance {
        private static final RainbowCache INSTANCE = new RainbowCache();
    }

    public void setCache(String key, Object cache) {
        setCachesVerify(key, cache);
        Map<String, Object> map = new HashMap<>(4);
        long now = System.currentTimeMillis();
        map.put(VALUE, cache);
        map.put(EXPIRATION, -1);
        map.put(CREATE_TIME, now);
        map.put(DESTROY_TIME, -1);
        this.caches.put(key, map);
    }

    /**
     * 设置具有过期时间的缓存
     * todo time unit unrealized
     *
     * @param key
     * @param cache
     * @param expiration
     * @param timeUnit
     */
    public void setCache(String key, Object cache, long expiration, TimeUnit timeUnit) {
        setCachesVerify(key, cache);
        Map<String, Object> map = new HashMap<>(4);
        long now = System.currentTimeMillis();
        map.put(VALUE, cache);
        map.put(EXPIRATION, expiration);
        map.put(CREATE_TIME, now);
        map.put(DESTROY_TIME, now + (expiration * 1000L));
        this.caches.put(key, map);
    }

    public void setCachesVerify(String key, Object cache) {
        setCachesVerify(key);
        if (cache == null) {
            throw new RuntimeException("value can not be null");
        }
    }
    public void setCachesVerify(String key) {
        if (key == null) {
            throw new RuntimeException("key can not be null");
        }
    }

    public Object getCache(String key) {
        setCachesVerify(key);
        triggerCleanUp(key);
        return this.caches.get(key).get(VALUE);
    }

    public String getCacheToString(String key) {
        setCachesVerify(key);
        triggerCleanUp(key);
        return this.caches.get(key).get(VALUE).toString();
    }

    public Integer getCacheToInteger(String key) {
        String obj = getCacheToString(key);
        return obj == null ? null : Integer.parseInt(obj);
    }

    public Double getCacheToDouble(String key) {
        String obj = getCacheToString(key);
        return obj == null ? null : Double.parseDouble(obj);
    }

    public Float getCacheToFloat(String key) {
        String obj = getCacheToString(key);
        return obj == null ? null : Float.parseFloat(obj);
    }

    public Character getCacheToCharacter(String key) {
        setCachesVerify(key);
        triggerCleanUp(key);
        Object obj = this.caches.get(key).get(VALUE);
        return obj == null ? null : (Character) obj;
    }

    public Short getCacheToShort(String key) {
        String obj = getCacheToString(key);
        return obj == null ? null : Short.parseShort(obj);
    }

    public Byte getCacheToByte(String key) {
        String obj = getCacheToString(key);
        return obj == null ? null : Byte.parseByte(obj);
    }

    public Long getCacheToLong(String key) {
        String obj = getCacheToString(key);
        return obj == null ? null : Long.parseLong(obj);
    }

    public <T> List<T> getCacheToList(String key) {
        setCachesVerify(key);
        triggerCleanUp(key);
        Object obj = this.caches.get(key).get(VALUE);
        return obj == null ? null : (List<T>) obj;
    }

    public <T> Set<T> getCacheToSet(String key) {
        setCachesVerify(key);
        triggerCleanUp(key);
        Object obj = this.caches.get(key).get(VALUE);
        return obj == null ? null : (Set<T>) obj;
    }

    public <T, E> Map<T, E> getCacheToMap(String key) {
        setCachesVerify(key);
        triggerCleanUp(key);
        Object obj = this.caches.get(key).get(VALUE);
        return obj == null ? null : (Map<T, E>) obj;
    }

    public Boolean getCacheExist(String key) {
        setCachesVerify(key);
        triggerCleanUp(key);
        return this.caches.get(key).get(VALUE) == null;
    }

    public Set<String> keys() {
        return caches.keySet();
    }

    public Set<String> keys(String key) {
        setCachesVerify(key);
        triggerCleanUp(key);
        Set<String> keys = new CopyOnWriteArraySet<>();
        caches.keySet().parallelStream().forEach(str -> {
            if (str.startsWith(key)) {
                keys.add(str);
            }
        });
        return keys;
    }

    public void delete(String key) {
        caches.remove(key);
    }

    public void delete(Set<String> set) {
        set.parallelStream().forEach(this::delete);
    }
}
