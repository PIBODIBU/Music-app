package com.android.simplemusic;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class TestActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private NotificationManager notificationManager;
    private static final int NOTIFY_ID = 1;

    private Toolbar toolbar;
    private CoordinatorLayout rootView;
    private View bottomSheetFrame;
    private AppCompatSeekBar seekBar;
    private MaterialProgressBar progressBar;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerViewAdapter songAdapter;
    private ListView songView;
    private ArrayList<Song> songs;

    private MediaPlayer mediaPlayer;
    private Random random;
    private Timer timer = new Timer();
    private TimerTask updateTask;

    private String songTitle = "";
    private int songPosition;
    private boolean shuffle = false;

    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rootView = (CoordinatorLayout) findViewById(R.id.root_view);
        seekBar = (AppCompatSeekBar) findViewById(R.id.seek_bar);
        progressBar = (MaterialProgressBar) findViewById(R.id.progress_bar);

        songPosition = 0;
        random = new Random();
        mediaPlayer = new MediaPlayer();

        setUpRecyclerView();
        initMusicPlayer();
        initBottomSheet();

        progressBar.setMax(100);
        progressBar.setProgress(50);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seek(progress);
                    Log.d(TAG, "onProgressChanged() -> User changed progress" +
                            "\nProgress: " + progress
                    );
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                setShuffle();
                if (getShuffle())
                    item.setIcon(R.drawable.ic_shuffle_teal_24dp);
                else
                    item.setIcon(R.drawable.ic_shuffle_white_24dp);
                break;
            case R.id.action_end:
                System.exit(0);
                break;
            case R.id.debug:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void songClicked(int position) {
        this.songPosition = position;
        playSong();
    }

    private void initBottomSheet() {
        bottomSheetFrame = rootView.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetFrame);
        //bottomSheetBehavior.setPeekHeight(100);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING: {
                        Log.d(TAG, "onStateChanged() -> New State: STATE_DRAGGING");

                        break;
                    }

                    case BottomSheetBehavior.STATE_SETTLING: {
                        Log.d(TAG, "onStateChanged() -> New State: STATE_SETTLING");

                        break;
                    }

                    case BottomSheetBehavior.STATE_EXPANDED: {
                        Log.d(TAG, "onStateChanged() -> New State: STATE_EXPANDED");

                        break;
                    }

                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        Log.d(TAG, "onStateChanged() -> New State: STATE_COLLAPSED");

                    }

                    case BottomSheetBehavior.STATE_HIDDEN: {
                        Log.d(TAG, "onStateChanged() -> New State: STATE_HIDDEN");

                        break;
                    }

                    default: {
                        break;
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    private void setUpRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        songs = new ArrayList<>();
        layoutManager = new LinearLayoutManager(this);
        songAdapter = new RecyclerViewAdapter(songs);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(songAdapter);

        songAdapter.setRecyclerViewCallbacks(new RecyclerViewAdapter.RecyclerViewCallbacks() {
            @Override
            public void songPicked(int position) {
                songClicked(position);
            }
        });

        getSongList();
        sortSongList();
    }

    private void sortSongList() {
        Collections.sort(songs, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    private void getSongList() {
        //query external audio
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //iterate over results if valid
        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songs.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }

        if (musicCursor != null) {
            musicCursor.close();
        }
    }

    public void initMusicPlayer() {
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                //start playback
                mediaPlayer.start();

                //notification
                Notification.Builder builder = new Notification.Builder(TestActivity.this);

                builder
                        .setSmallIcon(R.drawable.ic_play_arrow_white_24dp)
                        .setTicker(songTitle)
                        .setOngoing(true)
                        .setContentTitle("Playing")
                        .setContentText(songTitle);
                Notification notification = builder.build();
                notificationManager.notify(NOTIFY_ID, notification);
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //check if playback has reached the end of a track
                if (mediaPlayer.getCurrentPosition() > 0) {
                    mp.reset();
                    playNext();
                }
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.e(TAG, "Playback Error");
                mediaPlayer.reset();
                return false;
            }
        });
    }

    public void playSong() {
        //play
        mediaPlayer.reset();
        //get song
        Song playSong = songs.get(songPosition);
        //get title
        songTitle = playSong.getTitle();
        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try {
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e(TAG, "Error setting data source", e);
        }

        try {
            mediaPlayer.prepare();
        } catch (IOException ex) {
            Log.e(TAG, "Error setting data source", ex);
        }

        seekBar.setProgress(0);
        seekBar.setMax(getDuration());

        Log.d(TAG,
                "Duration: " + getDuration() +
                        "\nSeekBar max: " + seekBar.getMax()
        );

        startUpdateTask();
    }

    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (timer != null) {
            timer.cancel();
        }

        updateTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        Log.d(TAG, "updateTask -> Current position: " + mediaPlayer.getCurrentPosition());
                    }
                });
            }
        };

        timer = new Timer();
        timer.schedule(updateTask, 0, 1000);
    }

    private void stopUpdateTask() {
        if (timer != null)
            timer.cancel();
        if (updateTask != null)
            updateTask.cancel();
    }

    /**
     * Function to convert milliseconds time to
     * Timer Format
     * Hours:Minutes:Seconds
     */
    public String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    //set the song
    public void setSong(int songIndex) {
        songPosition = songIndex;
    }

    //playback methods
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void pausePlayer() {
        mediaPlayer.pause();
    }

    public void seek(int position) {
        mediaPlayer.seekTo(position);
    }

    public void start() {
        mediaPlayer.start();
    }

    public void playPrevious() {
        songPosition--;
        if (songPosition < 0) songPosition = songs.size() - 1;
        playSong();
    }

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

    @Override
    public void onDestroy() {
        mediaPlayer.stop();
        mediaPlayer.release();

        stopUpdateTask();

        notificationManager.cancel(NOTIFY_ID);

        super.onDestroy();
    }

    public void setShuffle() {
        if (shuffle)
            shuffle = false;
        else
            shuffle = true;
    }

    public boolean getShuffle() {
        return shuffle;
    }
}
