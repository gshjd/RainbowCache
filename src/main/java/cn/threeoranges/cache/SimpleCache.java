package cn.threeoranges.cache;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * @author: 李小熊
 **/
public class SimpleCache {

    /**
     * 本地缓存
     */
    private final Map<String, ValueObject> caches = new ConcurrentHashMap<>();
    /**
     * 定时清理锁
     */
    private boolean clearLock = false;

    private SimpleCache() {

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
     *
     * @param key key
     */
    public void triggerCleanUp(String key) {
        ValueObject valueObject = caches.get(key);
        if (valueObject == null) {
            return;
        }
        long destroyTime = valueObject.getDestroyTime();
        if (destroyTime != -1 && destroyTime <= System.currentTimeMillis()) {
            caches.remove(key);
        }
    }

    public static SimpleCache simpleCache() {
        return Instance.INSTANCE;
    }

    private static class Instance {
        private static final SimpleCache INSTANCE = new SimpleCache();
    }

    public void setCache(String key, Object cache) {
        ValueObject valueObject = new ValueObject(cache, -1L, -1L);
        this.caches.put(key, valueObject);
    }

    /**
     * 设置具有过期时间的缓存
     * todo time unit unrealized
     *
     * @param key        key
     * @param cache      cache
     * @param expiration expiration
     * @param timeUnit   timeUnit
     */
    public void setCache(String key, Object cache, long expiration, TimeUnit timeUnit) {
        long now = System.currentTimeMillis();
        ValueObject valueObject = new ValueObject(cache, -expiration, now + (expiration * 1000L));
        this.caches.put(key, valueObject);
    }

    public Object getCache(String key) {
        triggerCleanUp(key);
        ValueObject valueObject = caches.get(key);
        return valueObject == null ? null : valueObject.getValue();
    }

    public String getCacheToString(String key) {
        triggerCleanUp(key);
        ValueObject valueObject = caches.get(key);
        return valueObject == null ? null : valueObject.getValue().toString();
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
        triggerCleanUp(key);
        ValueObject valueObject = caches.get(key);
        return valueObject == null ? null : (Character) valueObject.getValue();
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

        triggerCleanUp(key);
        ValueObject valueObject = caches.get(key);
        return valueObject == null ? null : (List<T>) valueObject.getValue();
    }

    public <T> Set<T> getCacheToSet(String key) {

        triggerCleanUp(key);
        ValueObject valueObject = caches.get(key);
        return valueObject == null ? null : (Set<T>) valueObject.getValue();
    }

    public <T, E> Map<T, E> getCacheToMap(String key) {
        triggerCleanUp(key);
        ValueObject valueObject = caches.get(key);
        return valueObject == null ? null : (Map<T, E>) valueObject.getValue();
    }

    public Boolean getCacheExist(String key) {
        triggerCleanUp(key);
        ValueObject valueObject = caches.get(key);
        return valueObject != null && valueObject.getValue() != null;
    }

    public Set<String> keys() {
        return caches.keySet();
    }

    public int size() {
        return caches.size();
    }

    public Set<String> keys(String key) {

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
