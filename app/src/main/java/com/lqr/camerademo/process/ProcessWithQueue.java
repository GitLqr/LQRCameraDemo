package com.lqr.camerademo.process;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @创建者 LQR
 * @时间 19-11-12
 * @描述 使用Queue处理图像帧
 */
public class ProcessWithQueue extends Thread {

    private static final String TAG = "Queue";
    private LinkedBlockingQueue<byte[]> mQueue;

    public ProcessWithQueue(LinkedBlockingQueue<byte[]> frameQueue) {
        mQueue = frameQueue;
        start();
    }

    @Override
    public void run() {
        while (true) {
            byte[] frameData = null;
            try {
                frameData = mQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            processFrame(frameData);
        }
    }

    private void processFrame(byte[] frameData) {
        Log.i(TAG, "test");
    }
}
