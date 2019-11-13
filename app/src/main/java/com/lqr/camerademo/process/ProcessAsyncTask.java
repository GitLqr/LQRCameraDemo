package com.lqr.camerademo.process;

import android.os.AsyncTask;
import android.util.Log;

/**
 * @创建者 LQR
 * @时间 19-11-12
 * @描述 使用AsyncTask处理图像帧
 */
public class ProcessAsyncTask extends AsyncTask<byte[], Void, String> {

    private static final String TAG = "AsyncTask";

    @Override
    protected String doInBackground(byte[]... bytes) {
        processFrame(bytes[0]);
        return "test";
    }

    private void processFrame(byte[] frameData) {
        Log.i(TAG, "test");
    }
}
