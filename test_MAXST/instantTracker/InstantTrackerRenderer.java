package com.applepie4.artest.instantTracker;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;

import com.applepie4.artest.arobject.ColoredCube;
import com.applepie4.artest.arobject.VideoQuad;
import com.applepie4.artest.util.BackgroundRenderHelper;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.Trackable;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.TrackingResult;
import com.maxst.ar.TrackingState;
import com.maxst.videoplayer.VideoPlayer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class InstantTrackerRenderer implements GLSurfaceView.Renderer {

    private int surfaceWidth;
    private int surfaceHeight;
    private float posX;
    private float posY;
    private int mode;

    private BackgroundRenderHelper mBackgroundRenderHelper;
    private VideoQuad mVideoQuad;
    private ColoredCube mColoredCube;
    private Activity mActivity;

    public InstantTrackerRenderer(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        mBackgroundRenderHelper = new BackgroundRenderHelper();
        mBackgroundRenderHelper.init();

        mode = 1; // 0 - video, 1 - cube

        mVideoQuad = new VideoQuad();
        VideoPlayer player = new VideoPlayer(mActivity);
        mVideoQuad.setVideoPlayer(player);
        player.openVideo("Ifelse_demo.mp4");

        mColoredCube = new ColoredCube();

        MaxstAR.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;

        mVideoQuad.setScale(0.5f, -0.3f, 0.4f);

        MaxstAR.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);

        TrackingState state = TrackerManager.getInstance().updateTrackingState();
        TrackingResult trackingResult = state.getTrackingResult();
        mBackgroundRenderHelper.drawBackground();

        if (trackingResult.getCount() == 0)
            return;

        float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();

        Trackable trackable = trackingResult.getTrackable(0);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        if (mode == 0) {
            if (mVideoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_READY || mVideoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE)
                mVideoQuad.getVideoPlayer().start();

            mVideoQuad.setProjectionMatrix(projectionMatrix);
            mVideoQuad.setTransform(trackable.getPoseMatrix());
            mVideoQuad.setRotation(90.0f, 0.0f, 0.0f, 1.0f);
            mVideoQuad.setTranslate(posX, posY, 0.0f);
            mVideoQuad.setScale(0.5f, -0.3f, 0.4f);
            mVideoQuad.draw();
        } else {
            mColoredCube.setProjectionMatrix(projectionMatrix);
            mColoredCube.setTransform(trackable.getPoseMatrix());
            mColoredCube.setTranslate(posX, posY, 0.0f);
            mColoredCube.setScale(0.2f, 0.2f, 0.2f);
            mColoredCube.draw();
        }
    }

    void setTranslate(float x, float y) {
        posX += x;
        posY += y;
    }

    void resetPosition() {
        posX = 0;
        posY = 0;
    }
}
