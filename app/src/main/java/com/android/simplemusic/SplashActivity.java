package com.android.simplemusic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;

import com.bumptech.glide.Glide;

public class SplashActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();
    private static Context context;

    private AppCompatImageView IVsplash;
    private final long START_DELAY = 2 * 1000; // 2 sec

    private Thread splashThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(START_DELAY);
            } catch (InterruptedException ex) {
                Log.e(TAG, "Thread -> ", ex);
            } finally {
                startActivity(new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                );
                finish();
            }
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d(TAG, "onCreate()");

        context = this;
        IVsplash = (AppCompatImageView) findViewById(R.id.image_splash);

        Log.d(TAG, "onCreate() -> Loading splash image...");

        Glide
                .with(getApplicationContext())
                .load(R.drawable.nau)
                .crossFade()
                .into(IVsplash);

        Log.d(TAG, "onCreate() -> Starting new Thread...");

        splashThread.start();
    }
}
