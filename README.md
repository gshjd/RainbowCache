# RainbowCache

### 您的star是我继续前进的动力，如果喜欢请帮忙点个star

## 简介

一套缓存注解，缓存可选择使用本地缓存或redis缓存，内置利用redis完成的并发锁（使用并发锁注解一定要接入redis），提高开发效率，让开发人员更专注于业务。

该缓存在redis挂掉的情况下，会自动切换本地缓存，以防止击穿DB。

## POM引入

```xml
<dependency>
  <groupId>cn.threeoranges</groupId>
  <artifactId>RainbowCache</artifactId>
  <version>1.0</version>
</dependency>
```

## 注解使用

注解仅适用于Service实现层方法。

### 1. @RainbowCache

注解中含有key,dynamicKey,expiration,renew四个属性。

用于获取业务中的返回值并存入缓存中，若缓存中存在该key，则直接从缓存中获取。

#### key

String数组类型，固定键，可以有多个，缓存中固定不变的键。

#### dynamicKey

String类型，动态键，根据el表达式，获取指定参数得到的动态键。

`注：最终缓存键 = key + dynamicKey。`

#### expiration

long类型，缓存有效时间，默认为-1，永久不失效，单位为秒。

#### renew

boolean类型，是否需要续约，默认false，每当访问时自动续约时长。

### 2. @RainbowCachePut

注解中含有key,dynamicKey,expiration,renew四个属性。

每个属性和@RainbowCache注解一致，与@RainbowCache注解不同的是，该注解不会从缓存中读取数据，每次都会执行后续业务获取数据，更新缓存中的值。

### 3. @RainbowCacheClear

注解中含有key,dynamicKey,两个属性。

两个属性介绍和@RainbowCache注解一致。该注解仅用来清理缓存，支持范围清理，前缀为`key + dynamicKey`下的所有缓存都将被清除。

### 4. @RainbowDistributedLock

注解中含有key一个属性。

key属性表示改锁的唯一键，用来控制锁的唯一性。该注解会对方法进行加锁，业务执行完成以后解锁。

## 配置

### 1. rainbow.cache.type

缓存模式 simple或redis，默认simple（simple 本地缓存）

### 2. rainbow.cache.lock.time-out

分布式锁超时时间(毫秒)，默认30，单位为秒，-1为永不超时