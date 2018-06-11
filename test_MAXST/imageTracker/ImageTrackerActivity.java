package com.applepie4.artest.imageTracker;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.Toast;

import com.applepie4.artest.MAXSTARActivity;
import com.applepie4.artest.R;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.ResultCode;
import com.maxst.ar.TrackerManager;


public class ImageTrackerActivity extends MAXSTARActivity {

    private ImageTrackerRenderer mImageTrackerRenderer;
    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_tracker);

        mImageTrackerRenderer = new ImageTrackerRenderer(this);
        mGLSurfaceView = findViewById(R.id.image_gl_surface_view);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mImageTrackerRenderer);

        TrackerManager.getInstance().addTrackerData("ImageTarget/Ifelse.2dmap", true);
        TrackerManager.getInstance().addTrackerData("ImageTarget/bokhak_179.2dmap", true);
        TrackerManager.getInstance().setTrackingOption(TrackerManager.TrackingOption.EXTENDED_TRACKING);
        TrackerManager.getInstance().loadTrackerData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGLSurfaceView.onResume();
        TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_IMAGE);

        ResultCode resultCode = CameraDevice.getInstance().start(0, 1280, 720);

        if (resultCode != ResultCode.Success) {
            Toast.makeText(this, "카메라 열기 실패", Toast.LENGTH_SHORT).show();
            finish();
        }

        MaxstAR.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mImageTrackerRenderer.destroyVideoPlayer();
            }
        });

        mGLSurfaceView.onPause();

        TrackerManager.getInstance().stopTracker();
        CameraDevice.getInstance().stop();
        MaxstAR.onPause();
    }
}
