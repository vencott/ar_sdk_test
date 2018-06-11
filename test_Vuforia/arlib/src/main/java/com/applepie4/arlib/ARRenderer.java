package com.applepie4.arlib;

import android.app.Activity;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.applepie4.arlib.Vuforia.VuforiaRenderer;
import com.applepie4.arlib.Vuforia.VuforiaRendererControl;
import com.applepie4.arlib.Vuforia.VuforiaSession;
import com.applepie4.arlib.Vuforia.utils.Plane;
import com.applepie4.arlib.Vuforia.utils.Texture;
import com.applepie4.arlib.Vuforia.utils.TextureColorShaders;
import com.applepie4.arlib.Vuforia.utils.VuforiaMath;
import com.applepie4.arlib.Vuforia.utils.VuforiaUtils;
import com.applepie4.arlib.utils.LoadingDialogHandler;
import com.vuforia.Anchor;
import com.vuforia.AnchorResult;
import com.vuforia.Device;
import com.vuforia.DeviceTrackableResult;
import com.vuforia.Matrix34F;
import com.vuforia.Matrix44F;
import com.vuforia.PositionalDeviceTracker;
import com.vuforia.SmartTerrain;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.TrackerManager;
import com.vuforia.Vec2F;
import com.vuforia.Vec3F;
import com.vuforia.Vec4F;
import com.vuforia.VideoBackgroundConfig;
import com.vuforia.Vuforia;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Matrix44FInverse;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Matrix44FTranspose;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec3FAdd;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec3FCross;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec3FDot;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec3FNormalize;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec3FScale;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec3FSub;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec3FTransform;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec3FTransformNormal;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec4FDiv;
import static com.applepie4.arlib.Vuforia.utils.VuforiaMath.Vec4FTransform;
import static com.vuforia.Renderer.getInstance;

public class ARRenderer implements GLSurfaceView.Renderer, VuforiaRendererControl {
    public final static String MID_AIR_ANCHOR_NAME = "midAirAnchor";
    private static final String LOGTAG = "----ARRenderer----";
    public boolean mIsMidAirEnabled;
    Matrix44F mDevicePoseMatrix;
    Matrix44F mMidAirPoseMatrix;
    Anchor mMidAirAnchor;
    boolean mIsDeviceResultAvailable;
    boolean mSetObjectNewPosition;
    private VuforiaSession mVuforiaSession;
    private VuforiaRenderer mVuforiaRenderer;
    private Activity mActivity;
    private ARView mARView;
    private Plane mPlane;
    private int planeShaderProgramID;
    private int planeVertexHandle;
    private int planeTextureCoordHandle;
    private int planeMvpMatrixHandle;
    private int planeTexSampler2DHandle;
    private int planeColorHandle;
    private boolean mIsActive;
    private boolean mPlaceAnchorContent;

    private Vector<Texture> mTextures;
    private List<Boolean> mIsTextureToShow;

    private float[] objectMV;

    private float translate_X;
    private float translate_Y;
    private float translate_Z;
    private float scale_X;
    private float scale_Y;
    private float scale_Z;
    private float moving_X;
    private float moving_Y;
    private float moving_Z;

    // constructor
    public ARRenderer(Activity activity, ARView arView, VuforiaSession session) {
        mActivity = activity;
        mARView = arView;
        mVuforiaSession = session;
        mVuforiaRenderer = new VuforiaRenderer(this, mActivity, Device.MODE.MODE_AR, mVuforiaSession.getVideoMode(), false, 0.1f, 10f);
        mTextures = new Vector<>();
        mIsTextureToShow = new ArrayList<>();
        mIsActive = false;
        mIsMidAirEnabled = true;
        mPlaceAnchorContent = false;
    }

    // for GLSurfaceView.Renderer
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mVuforiaSession.onSurfaceCreated();
        mVuforiaRenderer.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mVuforiaSession.onSurfaceChanged(width, height);
        mVuforiaRenderer.onConfigurationChanged(mIsActive);
        initRendering();
    }

    private void initRendering() {
        Log.d(LOGTAG, "initRendering");
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

        for (Texture t : mTextures)
            loadTexture(t);

        Log.d(LOGTAG, "loadTextures");

        planeShaderProgramID = VuforiaUtils.createProgramFromShaderSrc(TextureColorShaders.TEXTURE_COLOR_VERTEX_SHADER, TextureColorShaders.TEXTURE_COLOR_FRAGMENT_SHADER);
        mPlane = new Plane();
        if (planeShaderProgramID > 0) {
            planeVertexHandle = GLES20.glGetAttribLocation(planeShaderProgramID, "vertexPosition");
            planeTextureCoordHandle = GLES20.glGetAttribLocation(planeShaderProgramID, "vertexTexCoord");
            planeMvpMatrixHandle = GLES20.glGetUniformLocation(planeShaderProgramID, "modelViewProjectionMatrix");
            planeTexSampler2DHandle = GLES20.glGetUniformLocation(planeShaderProgramID, "texSampler2D");
            planeColorHandle = GLES20.glGetUniformLocation(planeShaderProgramID, "uniformColor");
        } else
            Log.e(LOGTAG, "Could not init plane shader");

        mDevicePoseMatrix = VuforiaMath.Matrix44FIdentity();
        mMidAirPoseMatrix = VuforiaMath.Matrix44FIdentity();

        mIsDeviceResultAvailable = false;
        mSetObjectNewPosition = false;
        mIsMidAirEnabled = false;
        setTranslate(0, 0, 0);
        setScale(1, 1, 1);
        setMovingDistancePerFrame(0, 0, 0);

        finishLoadingAnimation();
    }

    public void finishLoadingAnimation() {
        Log.d(LOGTAG, "finishLoadingAnimation");
        mARView.getLoadingDialogHandler().sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive)
            return;
        mVuforiaRenderer.render();
    }

    // for VuforiaRendererControl, practical drawing
    @Override
    public void renderFrame(State state, float[] projectionMatrix, Matrix44F vbProjectionMatrix) {
        mVuforiaRenderer.renderVideoBackground();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        if (state.getNumTrackableResults() == 0) {

        } else {
            Matrix34F devicePoseTemp = new Matrix34F();
            boolean render2DImage = false;

            for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
                TrackableResult result = state.getTrackableResult(tIdx);
                Matrix44F modelViewMatrix = Tool.convertPose2GLMatrix(result.getPose());

                if (result.isOfType(DeviceTrackableResult.getClassType())) {
                    devicePoseTemp.setData(result.getPose().getData());
                    mDevicePoseMatrix = Matrix44FTranspose(Matrix44FInverse(modelViewMatrix));
                    mIsDeviceResultAvailable = true;
                    mIsMidAirEnabled = true;
                } else if (result.isOfType(AnchorResult.getClassType())) {
                    if (result.getTrackable().getName().equals(MID_AIR_ANCHOR_NAME)) {
                        render2DImage = true;
                        mMidAirPoseMatrix = modelViewMatrix;
                    }
                }

                if (!mIsDeviceResultAvailable)
                    continue;

                if (mPlaceAnchorContent) {
                    mSetObjectNewPosition = true;
                    mPlaceAnchorContent = false;
                }

                if (mSetObjectNewPosition) {
                    Matrix34F midAirPose = new Matrix34F();
                    midAirPose.setData(devicePoseTemp.getData());

                    Matrix34F translationMat = new Matrix34F();
                    float[] translationArray = new float[12];
                    translationArray[0] = 1;
                    translationArray[5] = 1;
                    translationArray[10] = 1;

                    translationMat.setData(translationArray);
                    Tool.setTranslation(translationMat, new Vec3F(0f, 0f, -3f));

                    midAirPose = Tool.multiply(midAirPose, translationMat);

                    createMidAirAnchor(midAirPose);
                    mSetObjectNewPosition = false;
                }
            }

            /*-------------------------2D 이미지를 그리는 부분-------------------------*/
            if (mIsDeviceResultAvailable && render2DImage) {
                objectMV = mMidAirPoseMatrix.getData();
                Matrix.rotateM(objectMV, 0, 90.0f, 0, 0, 1);
                Matrix.translateM(objectMV, 0, translate_X, translate_Y, translate_Z);
                Matrix.scaleM(objectMV, 0, scale_X, scale_Y, scale_Z);

                for (int i = 0; i < mTextures.size(); i++) {
                    if (mIsTextureToShow.get(i)) {
                        renderPlaneTexturedWithProjectionMatrix(projectionMatrix, objectMV, mTextures.get(i).mTextureID[0]);
                    }
                }
            }
            /*-------------------------2D 이미지를 그리는 부분-------------------------*/
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
    }

    private void renderPlaneTexturedWithProjectionMatrix(float[] projectionMatrix, float[] modelPoseMatrix, int textureHandle) {
        float[] modelViewProjection = new float[16];
        float[] poseMatrix = new float[16];

        Matrix.setIdentityM(poseMatrix, 0);

        Matrix.multiplyMM(poseMatrix, 0, mDevicePoseMatrix.getData(), 0, modelPoseMatrix, 0);
        /*-------------------------모델의 위치 / 크기 조정-------------------------*/
        translate_X += moving_X;
        translate_Y += moving_Y;
        translate_Z += moving_Z;
        Matrix.translateM(poseMatrix, 0, translate_X, translate_Y, translate_Z);
        Matrix.scaleM(poseMatrix, 0, scale_X, scale_Y, scale_Z);
        /*-------------------------모델의 위치 / 크기 조정-------------------------*/
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, poseMatrix, 0);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        GLES20.glEnableVertexAttribArray(planeVertexHandle);
        GLES20.glVertexAttribPointer(planeVertexHandle, 3, GLES20.GL_FLOAT, false, 0, mPlane.getVertices());

        GLES20.glEnableVertexAttribArray(planeTextureCoordHandle);
        GLES20.glVertexAttribPointer(planeTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mPlane.getTexCoords());

        GLES20.glUseProgram(planeShaderProgramID);
        GLES20.glUniformMatrix4fv(planeMvpMatrixHandle, 1, false, modelViewProjection, 0);
        GLES20.glUniform4f(planeColorHandle, 1, 1, 1, 1);
        GLES20.glUniform1i(planeTexSampler2DHandle, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, Plane.NUM_PLANE_INDEX, GLES20.GL_UNSIGNED_SHORT, mPlane.getIndices());

        GLES20.glDisableVertexAttribArray(planeTextureCoordHandle);
        GLES20.glDisableVertexAttribArray(planeVertexHandle);
        GLES20.glUseProgram(0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisable(GLES20.GL_BLEND);

        VuforiaUtils.checkGLError("renderPlaneTextured");
    }

    private void createMidAirAnchor(Matrix34F anchorPoseMatrix) {
        Log.i(LOGTAG, "Create Mid Air Anchor");

        TrackerManager trackerManager = TrackerManager.getInstance();
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker) trackerManager.getTracker(PositionalDeviceTracker.getClassType());

        if (mMidAirAnchor != null) {
            Log.i(LOGTAG, "Destroying hit test anchor with name " + MID_AIR_ANCHOR_NAME);
            boolean result = deviceTracker.destroyAnchor(mMidAirAnchor);
            Log.i(LOGTAG, "Hit test anchor " + (result ? "successfully destroyed" : "failed to destroy"));
        }

        mMidAirAnchor = deviceTracker.createAnchor(MID_AIR_ANCHOR_NAME, anchorPoseMatrix);

        if (mMidAirAnchor != null) {
            Log.i(LOGTAG, "Successfully created hit test anchor with name " + mMidAirAnchor.getName());
        } else {
            Log.e(LOGTAG, "Failed to create mid air anchor");
        }
    }

    // for onResume() in ARView, reset
    public void resetTrackers() {
        Log.i(LOGTAG, "resetTrackers");

        mDevicePoseMatrix = VuforiaMath.Matrix44FIdentity();
        mIsDeviceResultAvailable = false;

        TrackerManager trackerManager = TrackerManager.getInstance();
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker) trackerManager.
                getTracker(PositionalDeviceTracker.getClassType());
        SmartTerrain smartTerrain = (SmartTerrain) trackerManager.getTracker(SmartTerrain.getClassType());

        if (deviceTracker == null || smartTerrain == null) {
            Log.e(LOGTAG, "Failed to reset trackers, trackers not initialized");
            return;
        }

        deviceTracker.stop();
        smartTerrain.stop();

        deviceTracker.start();
        smartTerrain.start();
    }

    // for textures
    public void addTexture(Texture texture) {
        mTextures.add(texture);
        mIsTextureToShow.add(false);
    }

    public void loadTexture(Texture texture) {
        GLES20.glGenTextures(1, texture.mTextureID, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.mTextureID[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texture.mWidth, texture.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, texture.mData);
    }

    // for List<Boolean> mIsTextureToShow
    public void showTexture(int index) {
        if(index < mTextures.size())
            mIsTextureToShow.set(index, true);
    }

    public void hideTexture(int index) {
        if(index < mTextures.size())
            mIsTextureToShow.set(index, false);
    }

    // setter
    public void setActive(boolean active) {
        mIsActive = active;

        if (mIsActive)
            mVuforiaRenderer.configureVideoBackground();
    }

    public void setPlaceAnchorContent(boolean b) {
        Log.i(LOGTAG, "setPlaceAnchorContent");
        mPlaceAnchorContent = b;
    }

    public void setTranslate(float x, float y, float z) {
        Log.i(LOGTAG, "setTranslate");
        translate_X = x;
        translate_Y = y;
        translate_Z = z;
    }

    public void setScale(float x, float y, float z) {
        Log.i(LOGTAG, "setScale");
        scale_X = x;
        scale_Y = y;
        scale_Z = z;
    }

    public void setMovingDistancePerFrame(float x, float y, float z) {
        moving_X = x;
        moving_Y = y;
        moving_Z = z;
    }
}
