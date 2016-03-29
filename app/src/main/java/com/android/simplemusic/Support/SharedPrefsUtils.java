package com.android.simplemusic.Support;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPrefsUtils {

    private final String TAG = "SharedPrefsUtils";
    private static final String APP_PREFERENCES = "com.android.simplemusic.app_preferences";

    private Context context;
    private SharedPreferences sharedPreferences;

    private String KEY_IS_PLAYING = "KEY_IS_PLAYING";
    private String KEY_IS_PAUSED = "KEY_IS_PAUSED";

    public SharedPrefsUtils(Context context) {
        this.context = context;
        sharedPreferences = this.context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    public void savePlayingState(boolean isPlaying) {
        Log.d(TAG, "savePlayingState() -> isPaused: " + isPlaying);
        sharedPreferences.edit().putBoolean(KEY_IS_PLAYING, isPlaying).apply();
    }

    public void savePauseState(boolean isPaused) {
        Log.d(TAG, "savePauseState() -> isPaused: " + isPaused);
        sharedPreferences.edit().putBoolean(KEY_IS_PAUSED, isPaused).apply();
    }

    public boolean getPlayingState() {
        return sharedPreferences.getBoolean(KEY_IS_PLAYING, false);
    }

    public boolean getPauseState() {
        return sharedPreferences.getBoolean(KEY_IS_PAUSED, false);
    }
}
