package cn.threeoranges.thread.pool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: 李小熊
 * @date: 2021/3/12 11:07 上午
 **/
public class WindowTinyLruPool {
    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1, 3L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(Integer.MAX_VALUE), new ThreadPoolExecutor.AbortPolicy());

    private WindowTinyLruPool() {
    }

    public static WindowTinyLruPool windowTinyLru() {
        return Instance.INSTANCE;
    }

    public void execute(Runnable command) {
        executorService.execute(command);
    }

    private static class Instance {
        private static final WindowTinyLruPool INSTANCE = new WindowTinyLruPool();
    }
}
