package com.android.simplemusic;

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
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service {

    private static final String TAG = "MusicService";

    private MediaPlayer musicPlayer;
    private NotificationManager notificationManager;
    private ClipboardManager clipboardManager;
    private static AudioManager audioManager;
    private Notification notification;
    private static final int NOTIFY_ID = 1;

    private ArrayList<Song> songs;
    private Random random;

    private String songTitle = "";
    private String songArtist = "";
    private int songPosition;
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

        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");

        super.onRebind(intent);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        random = new Random(getRandomSeed());
        songPosition = 0;

        initMusicPlayer();

        songPosition = 0;
        songTitle = "National Aviation University";
        songArtist = "Simple MusicPlayer";
        makeNotification(R.drawable.ic_pause_white_24dp);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            Log.d(TAG, "getService()");

            return MusicService.this;
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

        startAsForeground();

        return START_NOT_STICKY;
    }

    private void startAsForeground() {
        startForeground(NOTIFY_ID, prepareNotification());
    }

    public void initMusicPlayer() {
        musicPlayer = new MediaPlayer();

        musicPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        musicPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // Start playback
                mediaPlayer.start();

                // Notification
                makeNotification(R.drawable.ic_play_arrow_white_24dp);
            }
        });
        musicPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Check if playback has reached the end of a track
                if (repeat) {
                    playSong();
                } else {
                    if (musicPlayer.getCurrentPosition() > 0) {
                        mp.reset();
                        playNext();
                    }
                }
            }
        });
        musicPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
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

    private void setSongPosition(int position) {
        this.songPosition = position;
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

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());

        builder
                .setSmallIcon(icon)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentIntent(intent)
                .setContentTitle(songArtist)
                .setContentText(songTitle);

        notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(NOTIFY_ID, notification);
    }

    private Notification prepareNotification() {
        if (notification != null) {
            notificationManager.cancel(NOTIFY_ID);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());

        builder
                .setSmallIcon(R.drawable.ic_pause_white_24dp)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentIntent(intent)
                .setContentTitle(songArtist)
                .setContentText(songTitle);

        notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        return notification;
    }

    /**
     * Music controls
     */
    /**
     * Play song at position {@link MusicService#getSongPosition}
     */
    private void playSong() {
        musicPlayer.reset();

        Song playSong = songs.get(getSongPosition());

        songTitle = playSong.getTitle();
        songArtist = playSong.getArtist();
        long currSong = playSong.getID();
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try {
            musicPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception ex) {
            Log.e(TAG, "Error setting data source", ex);
        }

        try {
            musicPlayer.prepare();
        } catch (IOException ex) {
            Log.e(TAG, "Error setting data source", ex);
        }
    }

    public void playSong(int position) {
        setSongPosition(position);

        musicPlayer.reset();

        Song playSong = songs.get(getSongPosition());

        songTitle = playSong.getTitle();
        songArtist = playSong.getArtist();
        long currSong = playSong.getID();
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try {
            musicPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception ex) {
            Log.e(TAG, "playSong() -> Error setting data source", ex);
        }

        try {
            musicPlayer.prepare();
        } catch (IOException ex) {
            Log.e(TAG, "playSong() -> Error setting data source", ex);
        } catch (IllegalStateException ex) {
            Log.e(TAG, "playSong() -> Error setting data source", ex);
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

    public void setPaused(boolean paused) {
        if (paused) {
            this.paused = true;

            musicPlayer.pause();

            makeNotification(R.drawable.ic_pause_white_24dp);
        } else {
            this.paused = false;

            musicPlayer.start();

            makeNotification(R.drawable.ic_play_arrow_white_24dp);
        }
    }

    public boolean getPausedState() {
        return paused;
    }

    public boolean getRepeatState() {
        return repeat;
    }

    public int getCurrentPosition() {
        return musicPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return musicPlayer.getDuration();
    }

    public void seek(int position) {
        musicPlayer.seekTo(position);
    }

    public void rewindForward(int interval) {
        seek(getCurrentPosition() + interval * 1000);
    }

    public void rewindBack(int interval) {
        seek(getCurrentPosition() - interval * 1000);
    }

    public void start() {
        musicPlayer.start();
    }

    public void playPrevious() {
        songPosition--;
        if (songPosition < 0) songPosition = songs.size() - 1;
        playSong();
    }

    public void stop() {
        musicPlayer.stop();

        makeNotification(R.drawable.ic_stop_white_24dp);
    }

    public boolean isPlaying() {
        return musicPlayer.isPlaying();
    }

    public boolean isPaused() {
        return paused;
    }

    public void setRepeatOn() {
        repeat = true;
    }

    public void setRepeatOff() {
        repeat = false;
    }

    public void setShuffleOn() {
        shuffle = true;
    }

    public void setShuffleOff() {
        shuffle = false;
    }

    public boolean getShuffleState() {
        return shuffle;
    }

}
