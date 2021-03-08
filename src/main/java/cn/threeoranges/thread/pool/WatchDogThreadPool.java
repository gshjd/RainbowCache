package cn.threeoranges.thread.pool;


import java.util.concurrent.*;

/**
 * @author: 李小熊
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
