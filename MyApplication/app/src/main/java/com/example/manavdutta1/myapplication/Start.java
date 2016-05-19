package com.example.manavdutta1.myapplication;

import android.os.Bundle;
import android.app.Activity;
import android.support.multidex.MultiDex;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import mhacks.autismassist.R;

public class Start extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mGestureDetector = new GestureDetector(this).setBaseListener(mBaseListener);
    }

    private final Handler mHandler = new Handler();

    /** Listener that displays the options menu when the touchpad is tapped. */
    private final GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {
        @Override
        public boolean onGesture(Gesture gesture) {
            if (gesture == Gesture.TAP) {
                startGame();
                return true;
            }
            if (gesture == Gesture.SWIPE_LEFT) {
                startGame();
                return true;
            }
            if (gesture == Gesture.SWIPE_RIGHT) {
                startGame();
                return true;
            }
            if (gesture == Gesture.LONG_PRESS) {
                startGame();
                return true;
            }
            else {
                return false;
            }
        }
    };

    /** Gesture detector used to present the options menu. */
    private GestureDetector mGestureDetector;
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector.onMotionEvent(event);
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
    private void startGame() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}