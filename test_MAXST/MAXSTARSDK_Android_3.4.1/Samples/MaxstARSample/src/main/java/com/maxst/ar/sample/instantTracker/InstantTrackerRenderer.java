/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.instantTracker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;

import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.MaxstARUtil;
import com.maxst.ar.Trackable;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.TrackingResult;
import com.maxst.ar.TrackingState;
import com.maxst.ar.sample.arobject.TexturedCube;
import com.maxst.ar.sample.arobject.VideoQuad;
import com.maxst.ar.sample.util.BackgroundRenderHelper;
import com.maxst.videoplayer.VideoPlayer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class InstantTrackerRenderer implements Renderer {

	public static final String TAG = InstantTrackerRenderer.class.getSimpleName();

	private int surfaceWidth;
	private int surfaceHeight;
	private BackgroundRenderHelper backgroundRenderHelper;

	private VideoQuad videoQuad_custom;

	private TexturedCube texturedCube;
	private float posX;
	private float posY;
	private Activity activity;

	InstantTrackerRenderer(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);

		TrackingState state = TrackerManager.getInstance().updateTrackingState();
		TrackingResult trackingResult = state.getTrackingResult();

		backgroundRenderHelper.drawBackground();

		if (trackingResult.getCount() == 0) {
			return;
		}

		float [] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();

		Trackable trackable = trackingResult.getTrackable(0);

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		texturedCube.setTransform(trackable.getPoseMatrix());
		texturedCube.setTranslate(posX, posY, -0.05f);
		//texturedCube.setProjectionMatrix(projectionMatrix);
		//texturedCube.draw();

		if (videoQuad_custom.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
				videoQuad_custom.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
			videoQuad_custom.getVideoPlayer().start();
		}
		videoQuad_custom.setProjectionMatrix(projectionMatrix);
		videoQuad_custom.setTransform(trackable.getPoseMatrix());
		videoQuad_custom.setRotation(90.0f, 0.0f, 0.0f, 1.0f);
		videoQuad_custom.setTranslate(0.0f, 0.0f, 0.0f);
		videoQuad_custom.setScale(0.5f, -0.3f, 0.4f);
		videoQuad_custom.draw();
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {

		surfaceWidth = width;
		surfaceHeight = height;

		texturedCube.setScale(0.3f, 0.3f, 0.1f);

		MaxstAR.onSurfaceChanged(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		backgroundRenderHelper = new BackgroundRenderHelper();
		backgroundRenderHelper.init();

		texturedCube = new TexturedCube();
		Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());
		texturedCube.setTextureBitmap(bitmap);

		videoQuad_custom = new VideoQuad();
		VideoPlayer player = new VideoPlayer(activity);
		videoQuad_custom.setVideoPlayer(player);
		player.openVideo("Ifelse_demo.mp4");

		MaxstAR.onSurfaceCreated();
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
