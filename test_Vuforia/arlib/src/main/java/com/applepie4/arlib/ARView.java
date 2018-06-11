/*
arlib 사용법
1. layout에 추가, findViewById
2. init
--------------------------------------
3. load
4. start(카메라 보이기 시작)
--------------------------------------
5. set
	setAugmentable
	setTanslate
	setMovingDistancePerFrame
6. show/hide
	image
	animation
--------------------------------------
7. stop
8. destroy
 */
package com.applepie4.arlib;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.applepie4.arlib.Vuforia.VuforiaControl;
import com.applepie4.arlib.Vuforia.VuforiaException;
import com.applepie4.arlib.Vuforia.VuforiaSession;
import com.applepie4.arlib.Vuforia.utils.Texture;
import com.applepie4.arlib.Vuforia.utils.VuforiaGLView;
import com.applepie4.arlib.utils.GifDecoder;
import com.applepie4.arlib.utils.LoadingDialogHandler;
import com.vuforia.CameraDevice;
import com.vuforia.DeviceTracker;
import com.vuforia.PositionalDeviceTracker;
import com.vuforia.SmartTerrain;
import com.vuforia.State;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

public class ARView extends FrameLayout implements VuforiaControl {
    private static final String LOGTAG = "----ARView----";

    // fundamental attributes
    private Context mContext;
    private Activity mActivity;
    private VuforiaSession mVuforiaSession;
    private VuforiaGLView mGlView;
    private ARRenderer mRenderer;
    private Vector<Texture> mTextures;
    private RelativeLayout mARLayout;
    private LoadingDialogHandler loadingDialogHandler;
    private boolean isTextureLoaded;
    private boolean isARStarted;
    // attributes for Animation
    private ARAnimationTask mARAnimationTask;
    private OnARAnimationListener mOnARAnimationListener;
    private int animationStartIndex;
    private int animationEndIndex;
    private int animationPlayNum;
    private Vector<Long> animationDurationList;
    // attributes for GIF
    private GifDecoder mGifDecoder;
    private InputStream mInputStream;
    private VuforiaException mException;

    // constructor
    public ARView(@NonNull Context context) {
        super(context);
    }

    public ARView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // init
    public void init(Context context, String key) {
        Log.d(LOGTAG, "init");
        mContext = context;
        mActivity = (Activity) context;
        mVuforiaSession = new VuforiaSession(this, CameraDevice.MODE.MODE_DEFAULT, key);
        mTextures = new Vector<>();
        loadingDialogHandler = new LoadingDialogHandler(mActivity);
        isTextureLoaded = false;
        isARStarted = false;
        mARAnimationTask = null;
        mOnARAnimationListener = null;
        mException = null;
        mVuforiaSession.initAR(mActivity, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // load image or images from assets
    public void loadImageFromAssets(String fileName) {
        if (mTextures == null)
            mTextures = new Vector<>();

        mTextures.add(Texture.loadTextureFromApk(fileName, mActivity.getAssets()));
        isTextureLoaded = true;
    }

    public void loadImagesFromAssets(List<String> imageNames) {
        Log.d(LOGTAG, "loadImagesFromAssets");
        if (mTextures == null)
            mTextures = new Vector<>();

        for (String imageName : imageNames)
            loadImageFromAssets(imageName);
    }

    public void loadImagesFromAssets(List<String> imageNames, long duration) {
        Log.d(LOGTAG, "loadImagesFromAssets");
        if (mTextures == null)
            mTextures = new Vector<>();

        for (String imageName : imageNames) {
            loadImageFromAssets(imageName);
            addAnimationDuration(duration);
        }
    }

    // load image or images from bitmap
    public void loadImageFromBitmap(Bitmap bitmap) {
        if (mTextures == null)
            mTextures = new Vector<>();

        mTextures.add(Texture.loadTextureFromBitmap(bitmap));
        isTextureLoaded = true;
    }

    public void loadImagesFromBitmap(List<Bitmap> bitmaps) {
        Log.d(LOGTAG, "loadImagesFromBitmap");
        if (mTextures == null)
            mTextures = new Vector<>();

        for (Bitmap bitmap : bitmaps)
            loadImageFromBitmap(bitmap);
    }

    public void loadImagesFromBitmap(List<Bitmap> bitmaps, long duration) {
        Log.d(LOGTAG, "loadImagesFromBitmap");
        if (mTextures == null)
            mTextures = new Vector<>();

        for (Bitmap bitmap : bitmaps) {
            loadImageFromBitmap(bitmap);
            addAnimationDuration(duration);
        }
    }

    // load gif
    public void loadGifFromInputStream(InputStream inputStream) {
        Log.d(LOGTAG, "loadGifFromInputStream");
        mGifDecoder = new GifDecoder();
        mGifDecoder.read(inputStream, 0);
        for (int i = 0; i < mGifDecoder.getFrameCount(); i++) {
            mGifDecoder.advance();
            Bitmap bitmap = mGifDecoder.getNextFrame();
            loadImageFromBitmap(bitmap);
            addAnimationDuration((long) mGifDecoder.getDelay(i));
        }
    }

    public void loadGifFromAssets(String gifFileName) {
        Log.d(LOGTAG, "loadGifFromAssets");
        mGifDecoder = new GifDecoder();
        mInputStream = null;
        try {
            mInputStream = mActivity.getAssets().open(gifFileName);
            mGifDecoder.read(mInputStream, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < mGifDecoder.getFrameCount(); i++) {
            mGifDecoder.advance();
            Bitmap bitmap = mGifDecoder.getNextFrame();
            loadImageFromBitmap(bitmap);
            addAnimationDuration((long) mGifDecoder.getDelay(i));
        }
    }

    // start
    public void start() {
        Log.d(LOGTAG, "start");
        if (mException == null && !isARStarted()) {
            isARStarted = true;
            startLoadingAnimation();
            initApplicationAR();
            mRenderer.setActive(true);
            mActivity.addContentView(mGlView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mARLayout.bringToFront();
            mARLayout.setBackgroundColor(Color.TRANSPARENT);

            try {
                mVuforiaSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (VuforiaException e) {
                Log.e(LOGTAG, e.getString());
            }

            boolean result = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");

        } else {
            if (mException != null)
                Log.e(LOGTAG, mException.getString());
        }
    }

    private void startLoadingAnimation() {
        Log.d(LOGTAG, "StartLoadingAnimation");
        mARLayout = (RelativeLayout) View.inflate(mContext, R.layout.layout_ar, null);
        mARLayout.setVisibility(VISIBLE);
        mARLayout.setBackgroundColor(Color.BLACK);
        loadingDialogHandler.mLoadingDialogContainer = mARLayout.findViewById(R.id.loading_indicator);
        loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        mActivity.addContentView(mARLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // stop
    public void stop() {
        Log.d(LOGTAG, "stop");
        if (isARStarted()) {
            onPause();
            if (mARAnimationTask != null)
                mARAnimationTask.cancel(true);
            if (mGlView.getParent() != null)
                ((ViewManager) mGlView.getParent()).removeView(mGlView);
            if (mARLayout.getParent() != null)
                ((ViewManager) mARLayout.getParent()).removeView(mARLayout);
            if (mTextures != null) {
                mTextures.clear();
                mTextures = null;
            }
            if (animationDurationList != null) {
                animationDurationList.clear();
                animationDurationList = null;
            }
            mGlView = null;
            mARLayout = null;
            mRenderer.setActive(false);
            mRenderer = null;
            mGifDecoder = null;
            mInputStream = null;
            isTextureLoaded = false;
            isARStarted = false;
        }
    }

    // for image augmentation
    public void showARImage() {
        if (mRenderer != null && isTextureLoaded()) {
            mRenderer.showTexture(0);
        }
    }

    public void showARImage(int index) {
        if (mRenderer != null && isTextureLoaded()) {
            mRenderer.showTexture(index);
        }
    }

    public void showARImage(int index, int x, int y, int z) {
        if (mRenderer != null && isTextureLoaded()) {
            mRenderer.setTranslate(x, y, z);
            mRenderer.showTexture(index);
        }
    }

    public void hideARImage(int index) {
        if (mRenderer != null && isTextureLoaded())
            mRenderer.hideTexture(index);
    }

    public void hideARImages() {
        Log.d(LOGTAG, "hideARImages");

        for (int i = 0; i < mTextures.size(); i++) {
            if (mRenderer != null && isTextureLoaded())
                mRenderer.hideTexture(i);
        }
    }

    // for animation augmention
    public void addAnimationDuration(long duration) {
        if (animationDurationList == null)
            animationDurationList = new Vector<>();

        animationDurationList.add(duration);
    }

    public void showARAnimation(int playNum, OnARAnimationListener onARAnimationListener) {
        Log.d(LOGTAG, "showARAnimation");
        if (mARAnimationTask != null) {
            mARAnimationTask.cancel(true);
            mARAnimationTask = null;
        }
        if (isARStarted() && isTextureLoaded()) {
            animationStartIndex = 0;
            animationEndIndex = mTextures.size() - 1;
            animationPlayNum = playNum;
            mOnARAnimationListener = onARAnimationListener;
            mARAnimationTask = new ARAnimationTask();
            mARAnimationTask.execute();
        }
    }

    public void showARAnimation(int playNum, int animationStartIndex, int animationEndIndex, OnARAnimationListener onARAnimationListener) {
        Log.d(LOGTAG, "showARAnimation");
        if (mARAnimationTask != null) {
            mARAnimationTask.cancel(true);
            mARAnimationTask = null;
        }
        if (isARStarted() && isTextureLoaded()) {
            this.animationStartIndex = animationStartIndex;
            this.animationEndIndex = animationEndIndex;
            animationPlayNum = playNum;
            mOnARAnimationListener = onARAnimationListener;
            mARAnimationTask = new ARAnimationTask();
            mARAnimationTask.execute();
        }
    }

    // getter and setter
    private boolean isTextureLoaded() {
        return isTextureLoaded;
    }

    public boolean isARStarted() {
        return isARStarted;
    }

    public void setAugmentable(boolean b) {
        if (mRenderer != null)
            mRenderer.setPlaceAnchorContent(b);
    }

    public void setTranslate(float x, float y) {
        if (mRenderer != null)
            mRenderer.setTranslate(x, y, 0);
    }

    public void setTranslate(float x, float y, float z) {
        if (mRenderer != null)
            mRenderer.setTranslate(x, y, z);
    }

    public void setScale(float scale) {
        if (mRenderer != null)
            mRenderer.setScale(scale, scale, scale);
    }

    public void setScale(float x, float y, float z) {
        if (mRenderer != null)
            mRenderer.setScale(x, y, z);
    }

    public void setMovingDistancePerFrame(float x, float y, float z) {
        if (mRenderer != null)
            mRenderer.setMovingDistancePerFrame(x, y, z);
    }

    public LoadingDialogHandler getLoadingDialogHandler() {
        return loadingDialogHandler;
    }

    // for VuforiaControl
    @Override
    public boolean doInitTrackers() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        DeviceTracker deviceTracker = (PositionalDeviceTracker) trackerManager.initTracker(PositionalDeviceTracker.getClassType());
        Tracker smartTerrain = trackerManager.initTracker(SmartTerrain.getClassType());

        if (deviceTracker != null)
            Log.i(LOGTAG, "Successfully initialized Device Tracker");
        else {
            Log.e(LOGTAG, "Failed to initialize Device Tracker");
            return false;
        }

        if (smartTerrain != null)
            Log.i(LOGTAG, "Successfully initialized Smart Terrain");
        else {
            Log.e(LOGTAG, "Failed to initialize Smart Terrain");
            return false;
        }

        return true;
    }

    @Override
    public boolean doLoadTrackersData() {
        return true;
    }

    @Override
    public boolean doStartTrackers() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker deviceTracker = trackerManager.getTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null && deviceTracker.start())
            Log.i(LOGTAG, "Successfully started Device Tracker");
        else {
            Log.e(LOGTAG, "Failed to start Device Tracker");
            return false;
        }

        Tracker smartTerrain = trackerManager.getTracker(SmartTerrain.getClassType());

        if (smartTerrain != null && smartTerrain.start())
            Log.i(LOGTAG, "Successfully started Smart Terrain");
        else {
            Log.e(LOGTAG, "Failed to start Smart Terrain");
            return false;
        }

        return true;
    }

    @Override
    public boolean doStopTrackers() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker deviceTracker = trackerManager.getTracker(PositionalDeviceTracker.getClassType());
        if (deviceTracker != null) {
            deviceTracker.stop();
            Log.i(LOGTAG, "Successfully stopped the Device Tracker");
        } else {
            Log.e(LOGTAG, "Failed to stop Device Tracker");
            return false;
        }

        Tracker smartTerrain = trackerManager.getTracker(SmartTerrain.getClassType());
        if (smartTerrain != null) {
            smartTerrain.stop();
            Log.i(LOGTAG, "Successfully stopped Smart Terrain");
        } else {
            Log.e(LOGTAG, "Failed to stop Smart Terrain");
            return false;
        }

        return true;
    }

    @Override
    public boolean doUnloadTrackersData() {
        return true;
    }

    @Override
    public boolean doDeinitTrackers() {
        TrackerManager trackerManager = TrackerManager.getInstance();

        if (trackerManager.deinitTracker(PositionalDeviceTracker.getClassType()))
            Log.i(LOGTAG, "Successfully deinit Device Tracker");
        else {
            Log.e(LOGTAG, "Failed to deinit Device Tracker");
            return false;
        }

        if (trackerManager.deinitTracker(SmartTerrain.getClassType()))
            Log.i(LOGTAG, "Successfully deinit Smart Terrain");
        else {
            Log.e(LOGTAG, "Failed to deinit Smart Terrain");
            return false;
        }

        return true;
    }

    @Override
    public void onInitARDone(VuforiaException exception) {
        Log.d(LOGTAG, "onInitARDone");

        if (exception == null)
            mException = null;
        else
            mException = exception;
    }

    private void initApplicationAR() {
        Log.d(LOGTAG, "initApplicationAR");
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new VuforiaGLView(mContext);
        mGlView.init(translucent, depthSize, stencilSize);
        mRenderer = new ARRenderer(mActivity, this, mVuforiaSession);
        if(mTextures == null)
            mTextures = new Vector<>();
        for (Texture texture : mTextures)
            mRenderer.addTexture(texture);
        mRenderer.setTranslate(0, 0, 0);
        mRenderer.setScale(1, 1, 1);
        mGlView.setRenderer(mRenderer);
        mGlView.setPreserveEGLContextOnPause(true);
    }

    @Override
    public void onVuforiaUpdate(State state) {

    }

    // for lifecycle
    public void onResume() {
        Log.d(LOGTAG, "onResume");
        try {
            mVuforiaSession.resumeAR();
        } catch (VuforiaException e) {
            Log.e(LOGTAG, e.getString());
        }

        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

        if (mRenderer != null)
            mRenderer.resetTrackers();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(LOGTAG, "onConfigurationChanged");
        mVuforiaSession.onConfigurationChanged();
    }

    public void onPause() {
        Log.d(LOGTAG, "onPause");
        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        try {
            mVuforiaSession.pauseAR();
        } catch (VuforiaException e) {
            Log.e(LOGTAG, e.getString());
        }
    }

    public void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        stop();
        try {
            mVuforiaSession.stopAR();
        } catch (VuforiaException e) {
            Log.e(LOGTAG, e.getString());
        }
        System.gc();
    }

    // other interface and class
    public interface OnARAnimationListener {
        void onARAnimationStarted();

        void onARAnimationFinished();
    }

    private class ARAnimationTask extends AsyncTask {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mOnARAnimationListener != null)
                mOnARAnimationListener.onARAnimationStarted();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            RepeatLoop:
            for (int playedNum = 0; playedNum < animationPlayNum; playedNum++) {
                showARImage(animationStartIndex);
                long frameStartedTime = SystemClock.elapsedRealtime();
                long frameElapsedTime;

                int index = animationStartIndex;
                while (index <= animationEndIndex) {
                    frameElapsedTime = SystemClock.elapsedRealtime() - frameStartedTime;
                    if (frameElapsedTime >= animationDurationList.get(index)) {
                        hideARImage(index);
                        index++;
                        if (index <= animationEndIndex)
                            showARImage(index);
                        frameStartedTime = SystemClock.elapsedRealtime();
                    }

                    if (this.isCancelled()) {
                        hideARImages();
                        break RepeatLoop;
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (mOnARAnimationListener != null)
                mOnARAnimationListener.onARAnimationFinished();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mOnARAnimationListener != null)
                mOnARAnimationListener.onARAnimationFinished();
        }
    }
}