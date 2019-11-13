package com.lqr.camerademo;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

/**
 * @创建者 LQR
 * @时间 19-11-11
 * @描述 图片视频查看界面
 */
public class ShowPhotoVideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        Uri uri = getIntent().getData();
        if (getIntent().getType().equals("image/*")) {
            ImageView view = new ImageView(this);
            view.setImageURI(uri);
            view.setLayoutParams(layoutParams);
            relativeLayout.addView(view);
        } else {
            MediaController mc = new MediaController(this);
            VideoView view = new VideoView(this);
            mc.setAnchorView(view);
            mc.setMediaPlayer(view);
            view.setMediaController(mc);
            view.setVideoURI(uri);
            view.start();
            view.setLayoutParams(layoutParams);
            relativeLayout.addView(view);
        }
        setContentView(relativeLayout);
    }
}
