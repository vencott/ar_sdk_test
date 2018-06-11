package com.applepie4.artest.instantTracker;

import com.applepie4.artest.MAXSTARActivity;
import com.applepie4.artest.R;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.ResultCode;
import com.maxst.ar.SensorDevice;
import com.maxst.ar.TrackerManager;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class InstantTrackerActivity extends MAXSTARActivity implements View.OnTouchListener {

    private InstantTrackerRenderer mInstantTrackerRenderer;
    private GLSurfaceView mGLSurfaceView;

    private static final float TOUCH_TOLERANCE = 5;
    private float touchStartX;
    private float touchStartY;
    private float translationX;
    private float translationY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instant_tracker);

        mInstantTrackerRenderer = new InstantTrackerRenderer(this);

        mGLSurfaceView = findViewById(R.id.instant_gl_surface_view);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mInstantTrackerRenderer);
        mGLSurfaceView.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) { // 드래그로 이동
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                touchStartX = x;
                touchStartY = y;

                final float[] screen = new float[2];
                screen[0] = x;
                screen[1] = y;

                final float[] world = new float[3];

                TrackerManager.getInstance().getWorldPositionFromScreenCoordinate(screen, world);
                translationX = world[0];
                translationY = world[1];
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                float dx = Math.abs(x - touchStartX);
                float dy = Math.abs(y - touchStartY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    touchStartX = x;
                    touchStartY = y;

                    final float[] screen = new float[2];
                    screen[0] = x;
                    screen[1] = y;

                    final float[] world = new float[3];

                    TrackerManager.getInstance().getWorldPositionFromScreenCoordinate(screen, world);
                    float posX = world[0];
                    float posY = world[1];

                    mInstantTrackerRenderer.setTranslate(posX - translationX, posY - translationY);
                    translationX = posX;
                    translationY = posY;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGLSurfaceView.onResume();
        SensorDevice.getInstance().start();
        TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_INSTANT);

        ResultCode resultCode = CameraDevice.getInstance().start(0, 1280, 720);

        if (resultCode != ResultCode.Success) {
            Toast.makeText(this, "카메라 열기 실패", Toast.LENGTH_SHORT).show();
            finish();
        }

        MaxstAR.onResume();

        TrackerManager.getInstance().findSurface();
        mInstantTrackerRenderer.resetPosition();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();

        TrackerManager.getInstance().quitFindingSurface();
        TrackerManager.getInstance().stopTracker();
        CameraDevice.getInstance().stop();
        SensorDevice.getInstance().stop();

        MaxstAR.onPause();
    }
}
