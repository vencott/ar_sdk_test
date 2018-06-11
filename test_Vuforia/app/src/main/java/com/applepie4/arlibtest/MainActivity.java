package com.applepie4.arlibtest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.applepie4.arlib.ARView;

import java.util.ArrayList;
import java.util.List;

// TODO : 오브젝트 여러개 증강
// TODO : z축 활용
// TODO : 스토리 만들어서 샘플 한두개 해보기

public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "----MainActivity----";
    private final static long DEFAULT_ANIMATION_DURATION = 200;
    private final static float TRANSLATE_X = 0f;
    private final static float TRANSLATE_Y = 0f;
    private final static float TRANSLATE_Z = 0f;
    private final static float MOVING_X = 0.000f;
    private final static float MOVING_Y = 0.000f;
    private final static float MOVING_Z = 0f;
    private final static int PLAY_NUM = 1;
    private final static int ANIMATION_START_INDEX = 0;
    private final static int ANIMATION_END_INDEX = 8;

    private final static String catDir = "png/cat/";
    private final static String gifFileName0 = "gif/hellopet_0.gif";
    private final static String gifFileName1 = "gif/hellopet_1.gif";
    private final static String gifFileName2 = "gif/hellopet_2.gif";

    private ARView mARView;
    private GestureDetector mGestureDetector;
    private List<String> imageNameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        setTheme(R.style.ARTheme_AppCompat);
        setContentView(R.layout.activity_main);

        mGestureDetector = new GestureDetector(this, new GestureListener());
        mARView = findViewById(R.id.ar_view);

        mARView.init(MainActivity.this, getString(R.string.VUFORIA_KEY));

        findViewById(R.id.onebutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadGif(gifFileName0);
            }
        });

        findViewById(R.id.twobutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadGif(gifFileName1);
            }
        });

        findViewById(R.id.threebutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadGif(gifFileName2);
            }
        });


        findViewById(R.id.startbutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mARView.start();
            }
        });
    }

    private void loadImage() {
        Log.d(LOGTAG, "loadImage");
        mARView.loadImageFromAssets(catDir + "cat_3.png");
    }

    private void loadImagesForAnimation() {
        Log.d(LOGTAG, "loadImagesForAnimation");

        imageNameList = new ArrayList<>();
        imageNameList.add(catDir + "cat_0.png");
        imageNameList.add(catDir + "cat_1.png");
        imageNameList.add(catDir + "cat_2.png");
        imageNameList.add(catDir + "cat_3.png");
        imageNameList.add(catDir + "cat_4.png");
        imageNameList.add(catDir + "cat_5.png");
        imageNameList.add(catDir + "cat_6.png");
        imageNameList.add(catDir + "cat_7.png");
        imageNameList.add(catDir + "cat_8.png");
        imageNameList.add(catDir + "cat_9.png");

        mARView.loadImagesFromAssets(imageNameList, DEFAULT_ANIMATION_DURATION);
    }

    private void loadGif(String gifFileName) {
        Log.d(LOGTAG, "loadGif");
        mARView.loadGifFromAssets(gifFileName);
    }

    private void showImage() {
        mARView.setAugmentable(true);
        mARView.setTranslate(TRANSLATE_X, TRANSLATE_Y, TRANSLATE_Z);
        mARView.setMovingDistancePerFrame(MOVING_X, MOVING_Y, MOVING_Z);
        mARView.showARImage(0);
    }

    private void showImage(int index) {
        mARView.setAugmentable(true);
        mARView.setTranslate(TRANSLATE_X, TRANSLATE_Y, TRANSLATE_Z);
        mARView.setMovingDistancePerFrame(MOVING_X, MOVING_Y, MOVING_Z);
        mARView.showARImage(index);
    }

    private void showAnimation() {
        mARView.setAugmentable(true);
        mARView.setTranslate(TRANSLATE_X, TRANSLATE_Y, TRANSLATE_Z);
        mARView.setMovingDistancePerFrame(MOVING_X, MOVING_Y, MOVING_Z);
        mARView.showARAnimation(PLAY_NUM, new ARView.OnARAnimationListener() {
            @Override
            public void onARAnimationStarted() {

            }

            @Override
            public void onARAnimationFinished() {

            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();
        if (mARView != null)
            mARView.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();
        if (mARView != null)
            mARView.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        if (mARView != null)
            mARView.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mARView != null && mARView.isARStarted())
            return mGestureDetector.onTouchEvent(event);
        else
            return false;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            showAnimation();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            mARView.stop();
        }
    }
}
