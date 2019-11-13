package com.lqr.camerademo.process;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @创建者 LQR
 * @时间 19-11-12
 * @描述 使用ThreadPool处理图像帧
 */
public class ProcessWithThreadPool {

    private static final String TAG = "ThreadPool";
    private static final int KEEP_ALIVE_TIME = 10;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private BlockingQueue<Runnable> mWorkQueue;
    private ThreadPoolExecutor mThreadPool;

    public ProcessWithThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize = corePoolSize * 2;
        mWorkQueue = new LinkedBlockingQueue<>();
        mThreadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, KEEP_ALIVE_TIME, TIME_UNIT, mWorkQueue);
    }

    public synchronized void post(final byte[] frameData) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                processFrame(frameData);
            }
        });
    }

    private void processFrame(byte[] frameData) {
        Log.i(TAG, "test");
    }
}
