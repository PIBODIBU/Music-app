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
import android.support.v4.content.ContextCompat;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.simplemusic.Support.AnimationSupport;
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import io.codetail.widget.RevealFrameLayout;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class TestActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private AudioManager audioManager;
    private NotificationManager notificationManager;
    private static final int NOTIFY_ID = 1;

    /**
     * View declaration
     */
    private Toolbar toolbar;
    private CoordinatorLayout rootView;

    // Bottom Bar
    private RelativeLayout RLopenBar;

    private MaterialProgressBar PBsongBar;

    private ImageButton IBplayStopBar;

    private TextView TVsongTitleBar;
    private TextView TVsongSubtitleBar;

    // Bottom Sheet Views
    private View bottomSheetFrame;

    private AppCompatSeekBar SBsongPosition;
    private AppCompatSeekBar SBvolume;

    private TextView TVtimeStampCurrent;
    private TextView TVtimeStampDuration;

    private TextView TVsongTitle;
    private TextView TVsongSubtitle;

    private ImageButton IBplayStop;
    private ImageButton IBrewindForward;
    private ImageButton IBrewindBack;
    private ImageButton IBnext;
    private ImageButton IBprevious;

    private ImageButton IBconrolVolume;
    private ImageButton IBconrolShuffle;
    private ImageButton IBconrolRepeat;
    private ImageButton IBconrolMenu;

    private RevealFrameLayout RFLcontainerSBvolume;
    private LinearLayout LLcontainerSBvolume;
    private ImageButton IBcontainerClose;

    /***/

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerViewAdapter songAdapter;
    private ArrayList<Song> songs;

    private MediaPlayer mediaPlayer;
    private Random random;
    private Timer timer = new Timer();
    private TimerTask updateTask;

    private String songTitle = "";
    private int songPosition;
    private boolean shuffle = false;
    private boolean paused = false;

    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        initLayout();

        setSupportActionBar(toolbar);
        songPosition = 0;
        random = new Random();

        setUpRecyclerView();
        initMusicPlayer();
        initBottomSheet();
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.onBackPressed();
        }
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
        setSong(position);
        playSong();
    }

    private void initLayout() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        rootView = (CoordinatorLayout) findViewById(R.id.root_view);
        PBsongBar = (MaterialProgressBar) findViewById(R.id.progress_bar);

        // Bottom Bar
        RLopenBar = (RelativeLayout) findViewById(R.id.bottom_bar_open);
        RLopenBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        TVsongTitleBar = (TextView) findViewById(R.id.bottom_bar_song_title);
        TVsongSubtitleBar = (TextView) findViewById(R.id.bottom_bar_song_subtitle);

        TVsongTitleBar.setSelected(true);
        TVsongSubtitleBar.setSelected(true);

        IBplayStopBar = (ImageButton) findViewById(R.id.bottom_bar_play_stop);
        IBplayStopBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paused) {
                    setPauseState(false);
                } else {
                    setPauseState(true);
                }
            }
        });

        // Bottom Sheet controls
        SBsongPosition = (AppCompatSeekBar) findViewById(R.id.seek_bar);
        SBsongPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

        TVtimeStampCurrent = (TextView) findViewById(R.id.time_stamp_current);
        TVtimeStampDuration = (TextView) findViewById(R.id.time_stamp_duration);

        TVsongTitle = (TextView) findViewById(R.id.bottom_sheet_song_title);
        TVsongSubtitle = (TextView) findViewById(R.id.bottom_sheet_song_subtitle);

        IBplayStop = (ImageButton) findViewById(R.id.bottom_sheet_play_stop);
        IBrewindForward = (ImageButton) findViewById(R.id.bottom_sheet_rewind_forward);
        IBrewindBack = (ImageButton) findViewById(R.id.bottom_sheet_rewind_back);
        IBnext = (ImageButton) findViewById(R.id.bottom_sheet_next);
        IBprevious = (ImageButton) findViewById(R.id.bottom_sheet_previous);

        LLcontainerSBvolume = (LinearLayout) findViewById(R.id.container_seek_bar_volume);
        RFLcontainerSBvolume = (RevealFrameLayout) findViewById(R.id.reveal_seek_bar_volume);
        IBcontainerClose = (ImageButton) findViewById(R.id.container_seek_bar_volume_close);

        IBcontainerClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimationSupport.Reveal.closeToLeft(LLcontainerSBvolume, new AnimationSupport.Reveal.AnimationAction() {
                    @Override
                    public void onPrepare() {
                    }

                    @Override
                    public void onStart() {
                        RFLcontainerSBvolume.setVisibility(View.GONE);
                    }
                });
            }
        });

        TVsongTitle.setSelected(true);
        TVsongSubtitle.setSelected(true);

        IBplayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paused) {
                    setPauseState(false);
                } else {
                    setPauseState(true);
                }
            }
        });
        IBnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });
        IBprevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });
        IBrewindForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rewindForward(5);
            }
        });
        IBrewindBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rewindBack(5);
            }
        });

        IBconrolVolume = (ImageButton) findViewById(R.id.bottom_sheet_control_volume);
        IBconrolVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimationSupport.Reveal.openFromLeft(LLcontainerSBvolume, new AnimationSupport.Reveal.AnimationAction() {
                    @Override
                    public void onPrepare() {
                        RFLcontainerSBvolume.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onStart() {
                    }
                });
            }
        });

        SBvolume = (AppCompatSeekBar) findViewById(R.id.seek_bar_volume);
        SBvolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        SBvolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            IBconrolVolume.setImageDrawable(ContextCompat.getDrawable(TestActivity.this, R.drawable.ic_volume_off_black_24dp));
        } else {
            IBconrolVolume.setImageDrawable(ContextCompat.getDrawable(TestActivity.this, R.drawable.ic_volume_up_black_24dp));
        }
        SBvolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "Volume SeekBar: " + progress);

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);

                if (progress == 0) {
                    IBconrolVolume.setImageDrawable(ContextCompat.getDrawable(TestActivity.this, R.drawable.ic_volume_off_black_24dp));
                } else {
                    IBconrolVolume.setImageDrawable(ContextCompat.getDrawable(TestActivity.this, R.drawable.ic_volume_up_black_24dp));

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

    private void initBottomSheet() {
        bottomSheetFrame = rootView.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetFrame);

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
        mediaPlayer = new MediaPlayer();

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
        mediaPlayer.reset();

        Song playSong = songs.get(songPosition);
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

        startUpdateTask(playSong);
    }

    private void setPauseState(boolean isPaused) {
        if (isPaused) {
            paused = true;
            Glide // Bottom Sheet
                    .with(TestActivity.this)
                    .load(R.drawable.ic_play_arrow_primary_24dp)
                    .into(IBplayStop);

            Glide //Bottom Bar
                    .with(TestActivity.this)
                    .load(R.drawable.ic_play_arrow_primary_24dp)
                    .into(IBplayStopBar);
            pause();
        } else {
            paused = false;
            Glide // Bottom Sheet
                    .with(TestActivity.this)
                    .load(R.drawable.ic_pause_primary_24dp)
                    .into(IBplayStop);
            Glide // Bottom Bar
                    .with(TestActivity.this)
                    .load(R.drawable.ic_pause_primary_24dp)
                    .into(IBplayStopBar);
            start();
        }
    }

    private void startUpdateTask(Song song) {
        prepareUpdateTask(song);

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
                        PBsongBar.setProgress(getCurrentPosition());
                        SBsongPosition.setProgress(getCurrentPosition());
                        TVtimeStampCurrent.setText(msToString(getCurrentPosition()));

                        Log.d(TAG, "updateTask -> Current position: " + msToString(getCurrentPosition()));
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

    private void prepareUpdateTask(Song song) {
        setPauseState(false);

        Log.d(TAG, "prepareUpdateTask() ->" +
                "\nArtist: " + song.getArtist() +
                "\nTitle: " + song.getTitle());

        // Bottom Bar
        PBsongBar.setProgress(0);
        PBsongBar.setMax(getDuration());

        TVsongTitleBar.setText(song.getTitle());
        TVsongSubtitleBar.setText(song.getArtist());

        // Bottom Sheet
        TVsongTitle.setText(song.getTitle());
        TVsongSubtitle.setText(song.getArtist());

        TVtimeStampDuration.setText(msToString(getDuration()));

        SBsongPosition.setProgress(0);
        SBsongPosition.setMax(getDuration());
    }

    /**
     * Method for converting milliseconds time to
     * Timer Format
     * Hours:Minutes:Seconds
     */
    public String msToString(long milliseconds) {
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

    public void seek(int position) {
        mediaPlayer.seekTo(position);
    }

    public void rewindForward(int interval) {
        seek(getCurrentPosition() + interval * 1000);
    }

    public void rewindBack(int interval) {
        seek(getCurrentPosition() - interval * 1000);
    }

    public void start() {
        mediaPlayer.start();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void stop() {
        mediaPlayer.stop();
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
