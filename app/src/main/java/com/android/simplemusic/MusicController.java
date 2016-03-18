package com.android.simplemusic;

import android.content.Context;
import android.support.v7.view.ContextThemeWrapper;
import android.widget.MediaController;

public class MusicController extends MediaController {

    public MusicController(Context context) {
        super(new ContextThemeWrapper(context, R.style.AppTheme_MusicPlayer));
    }

    @Override
    public void hide() {
        super.hide();
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void show(int timeout) {
        super.show(timeout);
    }
}

