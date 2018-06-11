package com.applepie4.artest.imageTracker;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

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

public class ImageTrackerRenderer implements GLSurfaceView.Renderer {
    private int surfaceWidth;
    private int surfaceHeight;

    private BackgroundRenderHelper mBackgroundRenderHelper;
    private VideoQuad mVideoQuad;
    private ColoredCube mColoredCube;
    private Activity mActivity;

    public ImageTrackerRenderer(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        mBackgroundRenderHelper = new BackgroundRenderHelper();
        mBackgroundRenderHelper.init();

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

        MaxstAR.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);

        TrackingState state = TrackerManager.getInstance().updateTrackingState();
        TrackingResult trackingResult = state.getTrackingResult();

        mBackgroundRenderHelper.drawBackground();

        boolean ifelseDetected = false;

        float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        for (int i = 0; i < trackingResult.getCount(); i++) {
            Trackable trackable = trackingResult.getTrackable(i);
            if (trackable.getName().equals("Ifelse")) {
                ifelseDetected = true;
                if (mVideoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                        mVideoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                    mVideoQuad.getVideoPlayer().start();
                }
                mVideoQuad.setProjectionMatrix(projectionMatrix);
                mVideoQuad.setTransform(trackable.getPoseMatrix());
                mVideoQuad.setTranslate(0.0f, 0.0f, 0.0f);
                mVideoQuad.setRotation(90.0f, 0.0f, 0.0f, 1.0f);
                mVideoQuad.setScale(1.04f, -0.9f, 0.0f);
                mVideoQuad.draw();
            } else if (trackable.getName().equals("bokhak_179")) {
                mColoredCube.setProjectionMatrix(projectionMatrix);
                mColoredCube.setTransform(trackable.getPoseMatrix());
                mColoredCube.setScale(1.0f, 1.0f, 1.0f);
                mColoredCube.draw();
            } else {
                mColoredCube.setProjectionMatrix(projectionMatrix);
                mColoredCube.setTransform(trackable.getPoseMatrix());
                mColoredCube.setScale(1.0f, 1.0f, 1.0f);
                mColoredCube.draw();
            }
        }

        if (!ifelseDetected) {
            if (mVideoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
                mVideoQuad.getVideoPlayer().pause();
            }
        }
    }

    public void destroyVideoPlayer() {
        mVideoQuad.getVideoPlayer().destroy();
    }
}
