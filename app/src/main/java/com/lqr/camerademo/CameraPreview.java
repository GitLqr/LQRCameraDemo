package com.lqr.camerademo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.lqr.camerademo.process.ProcessAsyncTask;
import com.lqr.camerademo.process.ProcessWithHandlerThread;
import com.lqr.camerademo.process.ProcessWithQueue;
import com.lqr.camerademo.process.ProcessWithThreadPool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @创建者 LQR
 * @时间 19-11-11
 * @描述 相机预览窗口
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String TAG = CameraPreview.class.getSimpleName();

    // 拍照录像
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    // 图像帧处理
    public static final int PROCESS_WITH_HANDLER_THREAD = 1;
    public static final int PROCESS_WITH_QUEUE = 2;
    public static final int PROCESS_WITH_ASYNC_TASK = 3;
    public static final int PROCESS_WITH_THREAD_POOL = 4;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    // 拍照录像
    private Uri mOutputMediaFileUri;
    private String mOutputMediaFileType;
    private MediaRecorder mMediaRecorder;

    // 缩放
    private float mOldDist = 1f; // 手指间距

    // 图像帧处理
    private int mProcessType = PROCESS_WITH_HANDLER_THREAD;
    private ProcessWithHandlerThread mProcessFrameHandlerThread;
    private Handler mProcessFrameHandler;
    private ProcessWithQueue mProcessFrameQueue;
    private LinkedBlockingQueue<byte[]> mFrameQueue;
    private ProcessWithThreadPool mProcessWithThreadPool;

    public CameraPreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);

        switch (mProcessType) {
            case PROCESS_WITH_HANDLER_THREAD:
                mProcessFrameHandlerThread = new ProcessWithHandlerThread("process frame");
                mProcessFrameHandler = new Handler(mProcessFrameHandlerThread.getLooper(), mProcessFrameHandlerThread);
                break;
            case PROCESS_WITH_QUEUE:
                mFrameQueue = new LinkedBlockingQueue<>();
                mProcessFrameQueue = new ProcessWithQueue(mFrameQueue);
                break;
            case PROCESS_WITH_ASYNC_TASK:
                break;
            case PROCESS_WITH_THREAD_POOL:
                mProcessWithThreadPool = new ProcessWithThreadPool();
                break;
        }
    }

    public Camera getCameraInstance() {
        if (mCamera == null) {
            // 不要在UI线程中执行Camera.open()！
            // try {
            //     mCamera = Camera.open();
            // } catch (Exception e) {
            //     e.printStackTrace();
            // }
            CameraHandlerThread cameraThread = new CameraHandlerThread("camera thread");
            synchronized (cameraThread) {
                cameraThread.openCamera();
            }
        }
        return mCamera;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        settingCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder.removeCallback(this);
        destroyCamera();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) { // 1个手指：对焦
            handleFocus(event, mCamera);
        } else { // 2个手指：缩放
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    mOldDist = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = getFingerSpacing(event);
                    if (newDist > mOldDist) {
                        handleZoom(true, mCamera);
                    } else {
                        handleZoom(false, mCamera);
                    }
                    mOldDist = newDist;
                    break;
            }
        }
        return true;
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        switch (mProcessType) {
            case PROCESS_WITH_HANDLER_THREAD:
                mProcessFrameHandler.obtainMessage(ProcessWithHandlerThread.WHAT_PROCESS_FRAME, data).sendToTarget();
                break;
            case PROCESS_WITH_QUEUE:
                try {
                    mFrameQueue.put(data);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case PROCESS_WITH_ASYNC_TASK:
                new ProcessAsyncTask().execute(data);
                break;
            case PROCESS_WITH_THREAD_POOL:
                mProcessWithThreadPool.post(data);
                break;
        }
    }

    /**
     * 初始化Camera
     */
    private void initCamera() {
        mCamera = getCameraInstance();
        mCamera.setPreviewCallback(this);
        try {
            mCamera.setPreviewDisplay(getHolder());
            mCamera.startPreview();
            // 配置Camera
            settingCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 配置Camera
     */
    private void settingCamera() {
        if (mCamera != null) {
            // 调整预览旋转方向
            int rotation = getDisplayOrientation();
            mCamera.setDisplayOrientation(rotation);
            // 调整拍照图像旋转方向
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(rotation);
            mCamera.setParameters(parameters);
            // 调整横纵比
            adjustDisplayRatio(rotation);
        }
    }

    /**
     * 销毁Camera
     */
    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        destroyCamera();
        mCameraId = mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK ?
                Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        initCamera();
    }

    private File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), TAG);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
            mOutputMediaFileType = "image/*";
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
            mOutputMediaFileType = "video/*";
        } else {
            return null;
        }
        mOutputMediaFileUri = Uri.fromFile(mediaFile);
        return mediaFile;
    }

    public Uri getOutputMediaFileUri() {
        return mOutputMediaFileUri;
    }

    public String getOutputMediaFileType() {
        return mOutputMediaFileType;
    }

    /**
     * 拍照
     */
    public void takePicture(final ImageView ivImage) {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, final Camera camera) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                        if (pictureFile == null) {
                            Log.d(TAG, "Error creating media file, check storage permissions");
                            return;
                        }
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                            if (ivImage != null) ivImage.setImageURI(mOutputMediaFileUri);
                            // 拍照之后，会停止预览，所以需要重新开启。
                            camera.startPreview();
                        } catch (FileNotFoundException e) {
                            Log.d(TAG, "File not found: " + e.getMessage());
                        } catch (IOException e) {
                            Log.d(TAG, "Error accessing file: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    /**
     * 开始录像
     */
    public boolean startRecording() {
        if (prepareVideoRecorder()) {
            mMediaRecorder.start();
            return true;
        } else {
            releaseMediaRecorder();
        }
        return false;
    }

    /**
     * 结束录像
     */
    public void stopRecording(final ImageView ivImage) {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (ivImage != null) {
                        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(mOutputMediaFileUri.getPath(), MediaStore.Video.Thumbnails.MINI_KIND);
                        ivImage.setImageBitmap(thumbnail);
                    }
                }
            });
        }
        releaseMediaRecorder();
    }

    /**
     * 是否正在录像
     *
     * @return
     */
    public boolean isRecording() {
        return mMediaRecorder != null;
    }

    private boolean prepareVideoRecorder() {
        mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String preVideoSize = prefs.getString(SettingsFragment.KEY_PREF_VIDEO_SIZE, "");
        String[] split = preVideoSize.split("x");
        mMediaRecorder.setVideoSize(Integer.parseInt(split[0]), Integer.parseInt(split[1]));

        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        mMediaRecorder.setPreviewDisplay(mHolder.getSurface());

        // 调整录像的旋转方向（在prepare前应用即可）
        // 注意正如setOrientationHint()中的Hint所表示的，
        // 视频的旋转并不是编码层面的旋转，视频帧数据并没有发生旋转，
        // 而只是在视频中增加了参数，希望播放器按照指定的旋转角度旋转后播放，
        // 所以具体效果因播放器而异
        int rotation = getDisplayOrientation();
        mMediaRecorder.setOrientationHint(rotation);

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.unlock();
        }
    }

    /**
     * 相机预览需要旋转的角度 = 相机预览目前的旋转角度 :&: 设备屏幕的旋转角度计算得到。
     *
     * @return
     */
    public int getDisplayOrientation() {
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);

        int result = (cameraInfo.orientation - degrees + 360) % 360;
        return result;
    }

    /**
     * 调整横纵比
     *
     * @param rotation 旋转角度
     */
    private void adjustDisplayRatio(int rotation) {
        // 得到父控件FrameLayout
        ViewGroup parent = (ViewGroup) getParent();
        Rect rect = new Rect();
        parent.getLocalVisibleRect(rect);
        // 父控件宽高
        int width = rect.width();
        int height = rect.height();
        // 得到预览分辨率
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        int previewWidth;
        int previewHeight;
        if (rotation == 90 || rotation == 270) {
            previewWidth = previewSize.height;
            previewHeight = previewSize.width;
        } else {
            previewWidth = previewSize.width;
            previewHeight = previewSize.height;
        }
        // 调整SurfaceView居中
        if (width * previewHeight > height * previewWidth) {
            final int scaledChildWidth = previewWidth * height / previewHeight;
            layout((width - scaledChildWidth) / 2, 0,
                    (width + scaledChildWidth) / 2, height);
        } else {
            final int scaledChildHeight = previewHeight * width / previewWidth;
            layout(0, (height - scaledChildHeight) / 2,
                    width, (height + scaledChildHeight) / 2);
        }
    }

    /**
     * 屏幕坐标转换为相机坐标 --> x,y in [-1000, 1000]
     *
     * @param x           触摸点x
     * @param y           触摸点y
     * @param coefficient
     * @param width
     * @param height
     * @return
     */
    private Rect calculateTapArea(float x, float y, float coefficient, int width, int height) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / width * 2000 - 1000);
        int centerY = (int) (y / height * 2000 - 1000);

        int halfAreaSize = areaSize / 2;
        RectF rectF = new RectF(clamp(centerX - halfAreaSize, -1000, 1000),
                clamp(centerY - halfAreaSize, -1000, 1000),
                clamp(centerX + halfAreaSize, -1000, 1000),
                clamp(centerY + halfAreaSize, -1000, 1000)
        );
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * 触摸对焦 & 触摸测光
     * <p>
     * 在得到转换后的矩形后就可以直接通过setFocusAreas()应用到相机了？实际没这么简单。
     * 直接这么做往往不能达到理想的效果，因为Android本身的问题以及设备的差异，
     * 在常用的对焦模式为continuous-picture下，setFocusAreas()可能会不工作。
     * 目前常用的解决办法是在setFocusAreas()同时修改相机对焦模式为macro等，
     * 待对焦完毕后，再将对焦模式修改为用户之前定义的。
     *
     * @param event
     * @param camera
     */
    private void handleFocus(MotionEvent event, Camera camera) {
        /*------------------ 触摸对焦：设置对焦区域 ------------------*/
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, viewWidth, viewHeight);

        camera.cancelAutoFocus();
        // 判断相机是否支持设定手动对焦点
        final Camera.Parameters params = camera.getParameters();
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 800));
            params.setFocusAreas(focusAreas);
        } else {
            Log.i(TAG, "focus areas not supported");
        }
        // 保存用户当前的对焦方式
        final String currentFocusMode = params.getFocusMode();
        // 将对焦方式修改为macro，应用到相机，相机开始对焦
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        camera.setParameters(params);

        // 当相机对焦完成后，还原用户设定
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocusMode);
                camera.setParameters(params);
            }
        });
        /*------------------ 触摸测光 ------------------*/
        Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f, viewWidth, viewHeight);
        // 判断相机是否支持设定手动测光点
        if (params.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 800));
            params.setMeteringAreas(meteringAreas);
        } else {
            Log.i(TAG, "metering areas not supported");
        }
    }

    /**
     * 计算2个手指之间的距离
     *
     * @param event
     * @return
     */
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 设置缩放
     *
     * @param isZoomIn
     * @param camera
     */
    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom(); // 最大缩放值
            int zoom = params.getZoom(); // 当前缩放值
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    private void openCameraOriginal() {
        try {
            mCamera = Camera.open(mCameraId);
        } catch (Exception e) {
            Log.d(TAG, "camera is not available");
        }
    }

    /**
     * 在非UI线程中创建Camera
     * <p>
     * 原因：{@link Camera.PreviewCallback#onPreviewFrame(byte[], Camera)} 的会调线程跟Camera.open()一致，会造成UI现成阻塞。
     */
    private class CameraHandlerThread extends HandlerThread {

        private final Handler mHandler;

        public CameraHandlerThread(String name) {
            super(name);
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    openCameraOriginal();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }
    }
}
