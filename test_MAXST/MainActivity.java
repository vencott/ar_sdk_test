package com.applepie4.artest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.applepie4.artest.imageTracker.ImageTrackerActivity;
import com.applepie4.artest.instantTracker.InstantTrackerActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mInstantTrackerButton;
    private Button mImageTrackerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInstantTrackerButton = findViewById(R.id.instant_tracker_button);
        mInstantTrackerButton.setOnClickListener(this);
        mImageTrackerButton = findViewById(R.id.image_tracker_button);
        mImageTrackerButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.instant_tracker_button:
                startActivity(new Intent(MainActivity.this, InstantTrackerActivity.class));
                break;
            case R.id.image_tracker_button:
                startActivity(new Intent(MainActivity.this, ImageTrackerActivity.class));
                break;
        }
    }
}
