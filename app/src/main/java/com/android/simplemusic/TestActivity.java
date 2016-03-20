package com.android.simplemusic;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.simplemusic.Support.AnimationSupport;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;

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

    private MusicIntentReceiver musicIntentReceiver;
    private ClipboardManager clipboardManager;
    private AudioManager audioManager;
    private NotificationManager notificationManager;
    private static final int NOTIFY_ID = 1;

    /**
     * View declaration
     */
    private Toolbar toolbar;
    private CoordinatorLayout rootView;

    // Bottom Bar
    private RevealFrameLayout RFLbottomBarReveal;
    private RelativeLayout RLbottomBarContainer;

    private RelativeLayout RLopenBar;

    private MaterialProgressBar PBsongBar;

    private ImageButton IBplayStopBar;

    private TextView TVsongTitleBar;
    private TextView TVsongSubtitleBar;

    private ImageView IValbumArtBar;

    // Bottom Sheet Views
    private View bottomSheetFrame;

    private AppCompatSeekBar SBsongPosition;
    private AppCompatSeekBar SBcontainerVolume;

    private TextView TVtimeStampCurrent;
    private TextView TVtimeStampDuration;

    private TextView TVsongTitle;
    private TextView TVsongSubtitle;

    private ImageButton IBcloseSheet;

    private ImageButton IBplayStop;
    private ImageButton IBrewindForward;
    private ImageButton IBrewindBack;
    private ImageButton IBnext;
    private ImageButton IBprevious;

    private ImageButton IBcontrolVolume;
    private ImageButton IBcontrolShuffle;
    private ImageButton IBcontrolRepeat;
    private ImageButton IBcontrolMenu;

    private RevealFrameLayout RFLcontainerSBvolume;
    private LinearLayout LLcontainerSBvolume;
    private ImageButton IBcontainerClose;

    private ImageView IValbumArt;

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

    private int rewindLenght = 5;
    private boolean shuffle = false;
    private boolean paused = false;
    private boolean repeat = false;

    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        musicIntentReceiver = new MusicIntentReceiver();
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        initLayout();

        setSupportActionBar(toolbar);
        songPosition = 0;
        random = new Random(getRandomSeed());

        setUpRecyclerView();
        initMusicPlayer();
        initBottomSheet();
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(musicIntentReceiver, filter);
        super.onResume();
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
        getMenuInflater().inflate(R.menu.test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                shuffleSongList();
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
        if (RFLbottomBarReveal.getVisibility() == View.GONE) {
            AnimationSupport.Reveal.openFromLeft(RLbottomBarContainer, new AnimationSupport.Reveal.AnimationAction() {
                @Override
                public void onPrepare() {
                    RFLbottomBarReveal.setVisibility(View.VISIBLE);
                }

                @Override
                public void onStart() {

                }
            });
        }
        setSong(position);
        playSong();
    }

    private void initLayout() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        rootView = (CoordinatorLayout) findViewById(R.id.root_view);
        PBsongBar = (MaterialProgressBar) findViewById(R.id.progress_bar);

        // Bottom Bar
        RFLbottomBarReveal = (RevealFrameLayout) findViewById(R.id.bottom_bar_reveal);
        RLbottomBarContainer = (RelativeLayout) findViewById(R.id.bottom_bar_container);

        IValbumArtBar = (ImageView) findViewById(R.id.bottom_bar_album_art);
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
        IValbumArt = (ImageView) findViewById(R.id.bottom_sheet_album_art);
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

        IBcloseSheet = (ImageButton) findViewById(R.id.bottom_sheet_close);
        IBcloseSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

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
                /*AnimationSupport.Reveal.closeToLeft(LLcontainerSBvolume, new AnimationSupport.Reveal.AnimationAction() {
                    @Override
                    public void onPrepare() {
                    }

                    @Override
                    public void onStart() {
                        RFLcontainerSBvolume.setVisibility(View.GONE);
                    }
                });*/

                // Don't touch this
                SBcontainerVolume.setVisibility(View.GONE);
                IBcontainerClose.setVisibility(View.GONE);

                LLcontainerSBvolume.setVisibility(View.INVISIBLE);
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
                rewindForward(rewindLenght);
            }
        });
        IBrewindBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rewindBack(rewindLenght);
            }
        });

        IBcontrolMenu = (ImageButton) findViewById(R.id.bottom_sheet_control_menu);
        IBcontrolMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(TestActivity.this, v);
                popupMenu.inflate(R.menu.control_menu);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_copy:
                                ClipData clip = ClipData.newPlainText("SimpleMusic_SongTitle", TVsongTitle.getText());
                                clipboardManager.setPrimaryClip(clip);
                                break;
                            case R.id.action_setting_rewind:
                                RewindLengthDialog rewindLengthDialog = new RewindLengthDialog();
                                rewindLengthDialog.init(TestActivity.this);
                                rewindLengthDialog.show(getSupportFragmentManager(), "rewindLengthDialog");
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });

                popupMenu.show();
            }
        });

        IBcontrolShuffle = (ImageButton) findViewById(R.id.bottom_sheet_control_shuffle);
        IBcontrolShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleShuffleState();
            }
        });

        IBcontrolRepeat = (ImageButton) findViewById(R.id.bottom_sheet_control_repeat);
        IBcontrolRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRepeatState();
            }
        });

        IBcontrolVolume = (ImageButton) findViewById(R.id.bottom_sheet_control_volume);
        IBcontrolVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Don't touch this
                SBcontainerVolume.setVisibility(View.VISIBLE);
                IBcontainerClose.setVisibility(View.VISIBLE);
                LLcontainerSBvolume.setVisibility(View.VISIBLE);

                /*AnimationSupport.Reveal.openFromLeft(LLcontainerSBvolume, new AnimationSupport.Reveal.AnimationAction() {
                    @Override
                    public void onPrepare() {
                        RFLcontainerSBvolume.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onStart() {
                    }
                });*/
            }
        });

        SBcontainerVolume = (AppCompatSeekBar) findViewById(R.id.seek_bar_volume);
        SBcontainerVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        SBcontainerVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        Log.d(TAG, "System -> " +
                "\nVolume max: " + audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) +
                "\nVolume current: " + (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));

        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(TestActivity.this, R.drawable.ic_volume_off_black_24dp));
        } else {
            IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(TestActivity.this, R.drawable.ic_volume_up_black_24dp));
        }
        SBcontainerVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "Volume SeekBar: " + progress);

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);

                if (progress == 0) {
                    IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(TestActivity.this, R.drawable.ic_volume_off_black_24dp));
                } else {
                    IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(TestActivity.this, R.drawable.ic_volume_up_black_24dp));

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

    private void shuffleSongList() {
        Collections.shuffle(songs, new Random(getRandomSeed()));
        songAdapter.notifyDataSetChanged();
    }

    private long getRandomSeed() {
        return System.currentTimeMillis();
    }

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri albumUri = android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        Cursor albumCursor = musicResolver.query(albumUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst() && albumCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int albumArtColumn = albumCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Albums.ALBUM_ART);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);

                String albumArt = "";
                try {
                    albumCursor.moveToPosition(musicCursor.getPosition());
                    albumArt = albumCursor.getString(albumArtColumn);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                songs.add(new Song(thisId, thisTitle, thisArtist, albumArt));
            }
            while (musicCursor.moveToNext());
        }

        if (musicCursor != null)
            musicCursor.close();
        if (albumCursor != null)
            albumCursor.close();
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

    private void startUpdateTask(Song song) {
        setPauseState(false);

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
        Log.d(TAG, "prepareUpdateTask() ->" +
                "\nArtist: " + song.getArtist() +
                "\nTitle: " + song.getTitle());

        // Bottom Bar
        IValbumArtBar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        try {
            if (song.getAlbumArt().equalsIgnoreCase("")) {
                IValbumArtBar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_music_note_primary_100_24dp));
            } else {
                Glide
                        .with(this)
                        .load(song.getAlbumArt())
                        .centerCrop()
                        .into(IValbumArtBar);
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            IValbumArtBar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_music_note_primary_100_24dp));
        }

        PBsongBar.setProgress(0);
        PBsongBar.setMax(getDuration());

        TVsongTitleBar.setText(song.getTitle());
        TVsongSubtitleBar.setText(song.getArtist());

        // Bottom Sheet
        IValbumArt.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        try {
            if (song.getAlbumArt().equalsIgnoreCase("")) {
                Glide
                        .with(this)
                        .load(R.drawable.ic_music_note_primary_100_100dp)
                        .into(IValbumArt);
            } else {
                Glide
                        .with(this)
                        .load(song.getAlbumArt())
                        .centerCrop()
                        .into(IValbumArt);
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();

            Glide
                    .with(this)
                    .load(R.drawable.ic_music_note_primary_100_100dp)
                    .into(IValbumArt);
        }

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

    private void setPauseState(boolean isPaused) {
        Log.d(TAG, "setPauseState() -> " + isPaused);
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

    private void toggleRepeatState() {
        if (repeat) {
            setRepeatOff();
        } else {
            setRepeatOn();
        }
    }

    private void setRepeatOn() {
        repeat = true;

        IBcontrolRepeat.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_primary_24dp));
    }

    private void setRepeatOff() {
        repeat = false;

        IBcontrolRepeat.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_black_24dp));
    }

    @Override
    public void onDestroy() {
        mediaPlayer.stop();
        mediaPlayer.release();

        stopUpdateTask();

        notificationManager.cancel(NOTIFY_ID);

        super.onDestroy();
    }

    public void toggleShuffleState() {
        if (shuffle) {
            setShuffleOff();
        } else {
            setShuffleOn();
        }
    }

    private void setShuffleOn() {
        shuffle = true;

        IBcontrolShuffle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_shuffle_primary_24dp));
    }

    private void setShuffleOff() {
        shuffle = false;

        IBcontrolShuffle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_shuffle_black_24dp));
    }

    public boolean getShuffle() {
        return shuffle;
    }

    public int getRewindLenght() {
        return rewindLenght;
    }

    public void setRewindLength(int length) {
        this.rewindLenght = length;
    }

    private class MusicIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Snackbar.make(rootView, "Headset is unplugged", Snackbar.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Snackbar.make(rootView, "Headset is plugged", Snackbar.LENGTH_SHORT).show();
                        break;
                    default:
                        Snackbar.make(rootView, "I have no idea what the headset state is", Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static class RewindLengthDialog extends DialogFragment {
        private final int OFFSET = 5;
        private AlertDialog dialog;
        private Activity activity;

        public void init(Activity activity) {
            this.activity = activity;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            View mainView = getActivity().getLayoutInflater().inflate(R.layout.dialog_rewind, null);
            AppCompatSeekBar seekBar = (AppCompatSeekBar) mainView.findViewById(R.id.seek_bar_rewind_lentgth);
            final TextView TVrewindLength = (TextView) mainView.findViewById(R.id.rewind_length);

            seekBar.setMax(30);
            seekBar.incrementProgressBy(OFFSET);
            seekBar.setProgress(((TestActivity) activity).getRewindLenght());

            TVrewindLength.setText(String.valueOf(((TestActivity) activity).getRewindLenght()));

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    progress = Math.round(progress / OFFSET) * OFFSET;
                    TVrewindLength.setText(String.valueOf(progress));
                    ((TestActivity) activity).setRewindLength(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    seekBar.showContextMenu();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            dialog = builder
                    .setTitle("Choose rewind length")
                    .setView(mainView)
                    .create();

            return dialog;
        }
    }
}
