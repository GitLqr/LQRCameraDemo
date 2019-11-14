package com.lqr.camerademo;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

public class MainActivity extends AppCompatActivity {

    private CameraPreview mPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getSupportActionBar().hide();
        this.getSupportActionBar().setBackgroundDrawable(null);
        this.getWindow().setBackgroundDrawable(null);
        // 屏幕常亮
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView ivMediaPreview = findViewById(R.id.iv_media_preview);
        // 设置
        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.camera_preview, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
        // 拍照
        findViewById(R.id.btn_capture_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.takePicture(ivMediaPreview);
            }
        });
        // 录像
        findViewById(R.id.btn_capture_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPreview.isRecording()) {
                    mPreview.stopRecording(ivMediaPreview);
                    ((Button) v).setText("录像");
                } else {
                    if (mPreview.startRecording()) {
                        ((Button) v).setText("停止");
                    }
                }
            }
        });
        // 切换Camera
        findViewById(R.id.btn_switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.switchCamera();
            }
        });
        // 预览
        ivMediaPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = mPreview.getOutputMediaFileUri();
                String type = mPreview.getOutputMediaFileType();
                if (uri != null && !TextUtils.isEmpty(type)) {
                    Intent intent = new Intent(MainActivity.this, ShowPhotoVideoActivity.class);
                    intent.setDataAndType(uri, type);
                    startActivityForResult(intent, 0);
                }
            }
        });
        requestPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @PermissionSuccess(requestCode = 100)
    public void onPermissionSuccess() {
        initCamera();
    }

    @PermissionFail(requestCode = 100)
    public void onPermissionFail() {
        Toast.makeText(getApplicationContext(), "请授权，否则无法正常使用", Toast.LENGTH_SHORT).show();
        finish();
        System.exit(0);
    }

    private void requestPermission() {
        PermissionGen.with(this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                )
                .request();
    }

    private void initCamera() {
        if (mPreview == null) {
            // 将相机预览控件动态添加进布局中，原因，因为Surface是实时创建的，最好通过代码动态添加。
            mPreview = new CameraPreview(MainActivity.this);
            FrameLayout flCameraPreview = findViewById(R.id.camera_preview);
            flCameraPreview.addView(mPreview);

            // 初始化相机设置默认值
            SettingsFragment.passCamera(mPreview.getCameraInstance());
            PreferenceManager.setDefaultValues(MainActivity.this, R.xml.preferences, false);
            SettingsFragment.setDefault(PreferenceManager.getDefaultSharedPreferences(MainActivity.this));
            SettingsFragment.init(PreferenceManager.getDefaultSharedPreferences(MainActivity.this));
        }
    }
}
