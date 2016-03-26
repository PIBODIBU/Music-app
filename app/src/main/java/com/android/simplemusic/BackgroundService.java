package com.android.simplemusic;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by root on 3/21/16.
 */
public class BackgroundService extends Service {

    private static final String TAG = "BackgroundService";

    private MediaPlayer mediaPlayer;
    private NotificationManager notificationManager;
    private ClipboardManager clipboardManager;
    private static AudioManager audioManager;
    private Notification notification;
    private static final int NOTIFY_ID = 1;

    private ArrayList<Song> songs;
    private Random random;

    private String songTitle = "";
    private int songPosition;
    private int rewindLenght = 5;
    private boolean shuffle = false;
    private boolean paused = true;
    private boolean repeat = false;
    private boolean wasPlugged = false;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");

        return new MusicBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");

        return false;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");

        super.onRebind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate()");

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        random = new Random(getRandomSeed());
        songPosition = 0;

        initMusicPlayer();
    }

    public class MusicBinder extends Binder {
        BackgroundService getService() {
            Log.d(TAG, "getService()");

            return BackgroundService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name) {
        Log.d(TAG, "stopService()");

        return super.stopService(name);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        return START_STICKY;
    }

    public void initMusicPlayer() {
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // Start playback
                mediaPlayer.start();

                // Notification
                makeNotification(R.drawable.ic_play_arrow_white_24dp);
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Check if playback has reached the end of a track
                if (repeat) {
                    playSong();
                } else {
                    if (mediaPlayer.getCurrentPosition() > 0) {
                        mp.reset();
                        playNext();
                    }
                }
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.e(TAG, "Playback Error: " + extra);
                mediaPlayer.reset();
                return true;
            }
        });
    }

    /**
     * Get seed for {@link Random}
     *
     * @return - (long) Seed, based on {@link System#currentTimeMillis}
     */
    private long getRandomSeed() {
        return System.currentTimeMillis();
    }

    /**
     * Set new {@link ArrayList} of {@link Song}
     *
     * @param songs - new {@link ArrayList}
     */
    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
    }

    /**
     * Get playing song position
     *
     * @return - (int) index of playing song
     */
    public int getSongPosition() {
        return songPosition;
    }

    /**
     * Make new {@link Notification} about play/pause state
     *
     * @param icon - Small icon of Notification
     */
    private void makeNotification(int icon) {
        if (notification != null) {
            notificationManager.cancel(NOTIFY_ID);
        }

        Intent notificationIntent = new Intent(this, TestActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());

        builder
                .setSmallIcon(icon)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentIntent(intent)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(NOTIFY_ID, notification);
    }

    /**
     * Music controls
     */
    /**
     * Play song at position {@link BackgroundService#getSongPosition}
     */
    public void playSong() {
        mediaPlayer.reset();

        Song playSong = songs.get(getSongPosition());

        songTitle = playSong.getTitle();
        long currSong = playSong.getID();

        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try {
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception ex) {
            Log.e(TAG, "Error setting data source", ex);
        }

        try {
            mediaPlayer.prepare();
        } catch (IOException ex) {
            Log.e(TAG, "Error setting data source", ex);
        }
    }

    /**
     * Check options (e.g. shuffle) and play next song from {@code songs}
     */
    public void playNext() {
        if (shuffle) {
            int newSong = songPosition;
            while (newSong == songPosition) {
                newSong = random.nextInt(songs.size());
            }
            songPosition = newSong;
        } else {
            songPosition++;
            if (songPosition >= songs.size()) songPosition = 0;
        }

        playSong();
    }
}
