package com.applepie4.arlib.Vuforia;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.applepie4.arlib.R;
import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.INIT_ERRORCODE;
import com.vuforia.INIT_FLAGS;
import com.vuforia.State;
import com.vuforia.Vuforia;

public class VuforiaSession implements Vuforia.UpdateCallbackInterface {
    private static String LOGTAG = "VuforiaSession";

    private Activity mActivity;
    private VuforiaControl mSessionControl;

    private boolean mStarted = false;
    private boolean mCameraRunning = false;

    private int mVideoMode = CameraDevice.MODE.MODE_DEFAULT;

    private InitVuforiaTask mInitVuforiaTask;
    private LoadTrackerTask mLoadTrackerTask;

    private Object mShutdownLock = new Object();

    private int mVuforiaFlags = 0;

    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;

    private String VUFORIA_LICENCE_KEY;

    public VuforiaSession(VuforiaControl vuforiaControl) {
        mSessionControl = vuforiaControl;
    }

    public VuforiaSession(VuforiaControl vuforiaControl, int videoMode) {
        mSessionControl = vuforiaControl;
        mVideoMode = videoMode;
    }

    public VuforiaSession(VuforiaControl vuforiaControl, int videoMode, String VUFORIA_LICENCE_KEY) {
        mSessionControl = vuforiaControl;
        mVideoMode = videoMode;
        this.VUFORIA_LICENCE_KEY = VUFORIA_LICENCE_KEY;
    }

    public void initAR(Activity activity, int screenOrientation) {
        VuforiaException vuforiaException = null;
        mActivity = activity;

        if ((screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR) && (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO))
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;

        OrientationEventListener orientationEventListener = new OrientationEventListener(mActivity) {
            int mLastRotation = -1;

            @Override
            public void onOrientationChanged(int orientation) {
                int activityRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
                if (mLastRotation != activityRotation)
                    mLastRotation = activityRotation;
            }
        };

        if (orientationEventListener.canDetectOrientation())
            orientationEventListener.enable();

        mActivity.setRequestedOrientation(screenOrientation);
        mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mVuforiaFlags = INIT_FLAGS.GL_20;

        if (mInitVuforiaTask != null) {
            String logMessage = "Cannot initialize SDK twice";
            vuforiaException = new VuforiaException(
                    VuforiaException.VUFORIA_ALREADY_INITIALIZATED,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException == null) {
            try {
                mInitVuforiaTask = new InitVuforiaTask();
                mInitVuforiaTask.execute();
            } catch (Exception e) {
                String logMessage = "Initializing Vuforia SDK failed";
                vuforiaException = new VuforiaException(
                        VuforiaException.INITIALIZATION_FAILURE,
                        logMessage);
                Log.e(LOGTAG, logMessage);
            }
        }

        if (vuforiaException != null)
            mSessionControl.onInitARDone(vuforiaException);
    }

    public void startAR(int camera) throws VuforiaException {
        String error;

        if (mCameraRunning) {
            error = "Camera already running, unable to open again";
            Log.e(LOGTAG, error);
            throw new VuforiaException(VuforiaException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        mCamera = camera;

        if (!CameraDevice.getInstance().init(camera)) {
            error = "Unable to open camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new VuforiaException(VuforiaException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        if (!CameraDevice.getInstance().selectVideoMode(mVideoMode)) {
            error = "Unable to set video mode";
            Log.e(LOGTAG, error);
            throw new VuforiaException(VuforiaException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        mSessionControl.doStartTrackers();

        if (!CameraDevice.getInstance().start()) {
            error = "Unable to start camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new VuforiaException(VuforiaException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        mCameraRunning = true;

        if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
        }
    }

    public void stopAR() throws VuforiaException {
        if (mInitVuforiaTask != null && mInitVuforiaTask.getStatus() != InitVuforiaTask.Status.FINISHED) {
            mInitVuforiaTask.cancel(true);
            mInitVuforiaTask = null;
        }

        if (mLoadTrackerTask != null && mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED) {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }

        mInitVuforiaTask = null;
        mLoadTrackerTask = null;

        mStarted = false;

        stopCamera();

        synchronized (mShutdownLock) {

            boolean unloadTrackersResult;
            boolean deinitTrackersResult;

            unloadTrackersResult = mSessionControl.doUnloadTrackersData();
            deinitTrackersResult = mSessionControl.doDeinitTrackers();
            Vuforia.deinit();

            if (!unloadTrackersResult)
                throw new VuforiaException(VuforiaException.UNLOADING_TRACKERS_FAILURE, "Failed to unload trackers\' data");

            if (!deinitTrackersResult)
                throw new VuforiaException(VuforiaException.TRACKERS_DEINITIALIZATION_FAILURE, "Failed to deinitialize trackers");
        }
    }

    public void resumeAR() throws VuforiaException {
        Vuforia.onResume();

        if (mStarted)
            startAR(mCamera);
    }

    public void pauseAR() throws VuforiaException {
        if (mStarted)
            stopCamera();

        Vuforia.onPause();
    }

    @Override
    public void Vuforia_onUpdate(State state) {
        mSessionControl.onVuforiaUpdate(state);
    }

    public void onConfigurationChanged() {
        Device.getInstance().setConfigurationChanged();
    }

    public void onResume() {
        Vuforia.onResume();
    }

    public void onPause() {
        Vuforia.onPause();
    }

    public void onSurfaceChanged(int width, int height) {
        Vuforia.onSurfaceChanged(width, height);
    }

    public void onSurfaceCreated() {
        Vuforia.onSurfaceCreated();
    }

    private String getInitializationErrorString(int code) {
        if (code == INIT_ERRORCODE.INIT_DEVICE_NOT_SUPPORTED)
            return mActivity.getString(R.string.INIT_ERROR_DEVICE_NOT_SUPPORTED);
        if (code == INIT_ERRORCODE.INIT_NO_CAMERA_ACCESS)
            return mActivity.getString(R.string.INIT_ERROR_NO_CAMERA_ACCESS);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_MISSING_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_MISSING_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_INVALID_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_INVALID_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_CANCELED_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_CANCELED_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH);
        else
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_UNKNOWN_ERROR);
    }

    public void stopCamera() {
        if (mCameraRunning) {
            mSessionControl.doStopTrackers();
            mCameraRunning = false;
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
        }
    }

    private boolean isARRunning() {
        return mStarted;
    }

    public int getVideoMode() {
        return mVideoMode;
    }

    private class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean> {
        private int mProgressValue = -1;

        @Override
        protected Boolean doInBackground(Void... voids) {
            synchronized (mShutdownLock) { // KEY
                Vuforia.setInitParameters(mActivity, mVuforiaFlags, VUFORIA_LICENCE_KEY);

                do {
                    mProgressValue = Vuforia.init();
                    publishProgress(mProgressValue);
                } while (!isCancelled() && mProgressValue >= 0 && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            VuforiaException vuforiaException = null;

            if (result) {
                Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: Vuforia " + "initialization successful");

                boolean initTrackersResult;
                initTrackersResult = mSessionControl.doInitTrackers();

                if (initTrackersResult) {
                    try {
                        mLoadTrackerTask = new LoadTrackerTask();
                        mLoadTrackerTask.execute();
                    } catch (Exception e) {
                        String logMessage = "Loading tracking data set failed";
                        vuforiaException = new VuforiaException(VuforiaException.LOADING_TRACKERS_FAILURE, logMessage);
                        Log.e(LOGTAG, logMessage);
                        mSessionControl.onInitARDone(vuforiaException);
                    }
                } else {
                    vuforiaException = new VuforiaException(VuforiaException.TRACKERS_INITIALIZATION_FAILURE, "Failed to initialize trackers");
                    mSessionControl.onInitARDone(vuforiaException);
                }
            } else {
                String logMessage;
                logMessage = getInitializationErrorString(mProgressValue);

                Log.e(LOGTAG, "InitVuforiaTask.onPostExecute: " + logMessage + " Exiting.");

                vuforiaException = new VuforiaException(VuforiaException.INITIALIZATION_FAILURE, logMessage);
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            synchronized (mShutdownLock) {
                return mSessionControl.doLoadTrackersData();
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            VuforiaException vuforiaException = null;

            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution " + (result ? "successful" : "failed"));

            if (!result) {
                String logMessage = "Failed to load tracker data.";

                Log.e(LOGTAG, logMessage);
                vuforiaException = new VuforiaException(VuforiaException.LOADING_TRACKERS_FAILURE, logMessage);
            } else {
                System.gc();

                Vuforia.registerCallback(VuforiaSession.this);

                mStarted = true;
            }
            mSessionControl.onInitARDone(vuforiaException);
        }
    }
}