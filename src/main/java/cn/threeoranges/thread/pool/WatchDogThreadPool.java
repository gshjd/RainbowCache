package cn.threeoranges.thread.pool;

import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.util.concurrent.*;

/**
 * @author: 李小熊
 * @date: 2021/3/4 4:06 下午
 **/
public class WatchDogThreadPool {

    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 3L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024), new ThreadPoolExecutor.AbortPolicy());

    private WatchDogThreadPool() {}

    public static WatchDogThreadPool getInstance() {
        return Instance.INSTANCE;
    }

    private ThreadPoolExecutor getExecutorService() {
        return executorService;
    }

    public void execute(Runnable command) {
        getExecutorService().execute(command);
    }

    private static class Instance {
        private static final WatchDogThreadPool INSTANCE = new WatchDogThreadPool();
    }
}
