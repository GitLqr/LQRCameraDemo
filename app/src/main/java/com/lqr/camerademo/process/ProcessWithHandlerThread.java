package com.lqr.camerademo.process;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * @创建者 LQR
 * @时间 19-11-12
 * @描述 使用HandlerThread处理图像帧
 */
public class ProcessWithHandlerThread extends HandlerThread implements Handler.Callback {

    private static final String TAG = "HandlerThread";
    public static final int WHAT_PROCESS_FRAME = 1;

    public ProcessWithHandlerThread(String name) {
        super(name);
        start();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case WHAT_PROCESS_FRAME:
                byte[] frameData = (byte[]) msg.obj;
                processFrame(frameData);
                return true;
            default:
                return false;
        }
    }

    private void processFrame(byte[] frameData) {
        Log.i(TAG, "test");
    }
}
