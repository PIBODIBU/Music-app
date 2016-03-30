package com.android.simplemusic;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.simplemusic.Support.AnimationSupport;
import com.android.simplemusic.Support.Chrome.CustomTabActivityHelper;
import com.android.simplemusic.Support.SharedPrefsUtils;
import com.bumptech.glide.Glide;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import io.codetail.widget.RevealFrameLayout;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private SharedPrefsUtils sharedPrefsUtils;
    private MusicRetriever musicRetriever;
    private MusicIntentReceiver musicIntentReceiver = new MusicIntentReceiver();
    private ClipboardManager clipboardManager;
    private static AudioManager audioManager;

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

    // Bottom Sheet Primary Views
    private View bottomSheetFrame;

    private ImageView IVPlayerMain;

    private TextView TVtimeStampCurrent;
    private TextView TVtimeStampDuration;

    private TextView TVsongTitle;
    private TextView TVsongSubtitle;

    private ImageButton IBplay;
    private ImageButton IBpause;
    private ImageButton IBnext;
    private ImageButton IBprevious;
    private ImageButton IBstop;

    private static ImageButton IBcontrolVolume;
    private ImageButton IBcontrolShuffle;
    private ImageButton IBcontrolRepeat;
    private ImageButton IBcontrolMenu;

    /***/

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerViewAdapter songAdapter;
    private ArrayList<Song> songs;

    private Timer timer = new Timer();
    private TimerTask updateTask;

    private boolean wasPlugged = false;
    private boolean isServiceBound = false;

    private BottomSheetBehavior bottomSheetBehavior;

    // Save instance
    private final String TAG_WAS_PLAYING = "TAG_WAS_PLAYING";
    private final String TAG_WAS_PAUSED = "TAG_WAS_PAUSED";
    private boolean INSTANCE_WAS_PLAYING = false;
    private boolean INSTANCE_WAS_PAUSED = false;

    // Service
    private MusicService musicService = new MusicService();
    private Intent musicIntent;

    /**
     * Base connection to {@link MusicService}
     */
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                // Casting service to MusicService.MusicBinder type
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                // Getting Service from Binder
                musicService = binder.getService();

                // Passing new DataSet to Service
                musicService.setSongs(songs);

                // Triggering boolean variable
                isServiceBound = true;

                Log.d(TAG, "onServiceConnected() -> Service Connected");

                // Checking if orientation was changed
                Log.d(TAG, "\nonServiceConnected() -> " +
                        "\nINSTANCE_WAS_PLAYING: " + INSTANCE_WAS_PLAYING +
                        "\nINSTANCE_WAS_PAUSED: " + INSTANCE_WAS_PAUSED);

                if (INSTANCE_WAS_PLAYING || INSTANCE_WAS_PAUSED) {
                    startUpdateTask(songs.get(musicService.getSongPosition()));
                    openBottomBar();
                }

                Log.d(TAG, "\nonServiceConnected() -> " +
                        "\nsharedPrefsUtils.getPlayingState(): " + sharedPrefsUtils.getPlayingState() +
                        "\nsharedPrefsUtils.getPauseState(): " + sharedPrefsUtils.getPauseState());

                if (musicService.getSongPosition() != -1) {
                    if (sharedPrefsUtils.getPlayingState()) {
                        startUpdateTask(songs.get(musicService.getSongPosition()));
                        openBottomBar();
                    } else if (sharedPrefsUtils.getPauseState()) {
                        refreshUpdateTask(songs.get(musicService.getSongPosition()));
                        openBottomBar();
                    }
                } else {
                    Log.e(TAG, "onServiceConnected() -> musicService.getSongPosition() == -1");
                }

            } catch (Exception ex) {
                Log.e(TAG, "onServiceConnected() -> Error Connecting Service", ex);

                isServiceBound = false;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;

            Log.d(TAG, "Service -> Service Disconnected");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefsUtils = new SharedPrefsUtils(this);
        musicIntent = new Intent(this, MusicService.class);
        musicRetriever = new MusicRetriever(this);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initLayout();
        setSupportActionBar(toolbar);
        initBottomSheet();
        setUpRecyclerView();
        checkMute();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (savedInstanceState != null) {
            // Using variable because musicService will be initialized only in onServiceConnected()
            Log.d(TAG, "\nonPostCreate() -> " +
                    "\nTAG_WAS_PLAYING: " + savedInstanceState.getBoolean(TAG_WAS_PLAYING, false) +
                    "\nTAG_WAS_PAUSED: " + savedInstanceState.getBoolean(TAG_WAS_PAUSED, false));

            if (savedInstanceState.getBoolean(TAG_WAS_PLAYING, false)) {
                INSTANCE_WAS_PLAYING = true;
            }
            if (savedInstanceState.getBoolean(TAG_WAS_PAUSED, false)) {
                INSTANCE_WAS_PAUSED = true;
            }
        } else {
            Log.e(TAG, "onPostCreate() -> savedInstanceState in null");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Binding Service
        bindService(musicIntent, musicConnection, Context.BIND_AUTO_CREATE);

        // Starting Service
        startService(musicIntent);
    }

    @Override
    protected void onStop() {
        // Unbind Service first
        unbindService(musicConnection);

        super.onStop();
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(musicIntentReceiver, filter);

        super.onResume();

        checkMute();
    }

    @Override
    public void onDestroy() {
        stopUpdateTask();
        unregisterReceiver(musicIntentReceiver);

        /**
         * Saving current state of playback
         * <p/>
         * <p>
         * sharedPrefsUtils used for saving persistent state.
         * Because of using {@link MusicService} we should save (boolean) playing state.
         * i.e. we save {@link MusicService#isPlaying()}
         * </p>
         */
        sharedPrefsUtils.savePauseState(musicService.isPaused());
        sharedPrefsUtils.savePlayingState(musicService.isPlaying());

        super.onDestroy();
    }

    /**
     * Saving current state of playback
     * <p/>
     * <p>
     * onSaveInstanceState() used for saving non-persistent state.
     * Because of using {@link MusicService} we should save (boolean) playing state.
     * i.e. we save {@link MusicService#isPlaying()}
     * </p>
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "onSaveInstanceState()");

        outState.putBoolean(TAG_WAS_PLAYING, musicService.isPlaying());
        outState.putBoolean(TAG_WAS_PAUSED, musicService.isPaused());

        Log.d(TAG, "\nonSaveInstanceState() -> " +
                "\nmusicService.isPlaying(): " + musicService.isPlaying() +
                "\nmusicService.isPaused(): " + musicService.isPaused());
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
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                shuffleSongList();
                break;
            case R.id.action_end:
                stopService(musicIntent);
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Implementing base logic of clicking on {@link RecyclerView} item (song)
     *
     * @param position - Position of clicked item
     */
    public void songClicked(int position) {
        if (isServiceBound) {
            musicService.playSong(position);

            openBottomBar();

            startUpdateTask(songs.get(position));
        } else {
            // Service is not bound. Showing warning
            showWarning();
        }
    }

    /**
     * Basic warning about non-availability of connection with {@link android.app.Service}. In our case it's {@link MusicService}
     */
    private void showWarning() {
        Toast.makeText(getApplicationContext(), "Service is not bound", Toast.LENGTH_SHORT).show();
    }

    /**
     * Check current visibility and open BottomBar with
     * {@link com.android.simplemusic.Support.AnimationSupport.Reveal} animation
     */
    private void openBottomBar() {
        if (RLbottomBarContainer.getVisibility() != View.VISIBLE) {
            AnimationSupport.Reveal.openFromLeft(RLbottomBarContainer, new AnimationSupport.Reveal.AnimationCallbacks() {
                @Override
                public void onPrepare() {
                    RFLbottomBarReveal.setVisibility(View.VISIBLE);
                }

                @Override
                public void onStart() {

                }
            });
        }
    }

    /**
     * Method for closing BottomBar
     */
    private void closeBottomBar() {
        AnimationSupport.Reveal.closeToLeft(RLbottomBarContainer, null);
    }

    /**
     * Method for initialization Views, {@link android.view.View.OnClickListener},
     * base logic of UI
     */
    private void initLayout() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        rootView = (CoordinatorLayout) findViewById(R.id.root_view);

        // Bottom Bar
        PBsongBar = (MaterialProgressBar) findViewById(R.id.progress_bar);

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
                if (musicService.isPaused()) {
                    setPaused(false);
                } else {
                    setPaused(true);
                }
            }
        });

        // Bottom Sheet Primary
        IVPlayerMain = (ImageView) findViewById(R.id.player_image_main);
        Glide
                .with(this)
                .load(R.drawable.player_main)
                .crossFade()
                .into(IVPlayerMain);

        TVtimeStampCurrent = (TextView) findViewById(R.id.time_stamp_current);
        TVtimeStampDuration = (TextView) findViewById(R.id.time_stamp_duration);

        TVsongTitle = (TextView) findViewById(R.id.bottom_sheet_song_title);
        TVsongSubtitle = (TextView) findViewById(R.id.bottom_sheet_song_subtitle);

        IBplay = (ImageButton) findViewById(R.id.bottom_sheet_play_stop);
        IBpause = (ImageButton) findViewById(R.id.bottom_sheet_pause);
        IBnext = (ImageButton) findViewById(R.id.bottom_sheet_next);
        IBprevious = (ImageButton) findViewById(R.id.bottom_sheet_previous);
        IBstop = (ImageButton) findViewById(R.id.bottom_sheet_stop);

        TVsongTitle.setSelected(true);
        TVsongSubtitle.setSelected(true);

        IBstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicService.stop();
                closeBottomBar();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        IBplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService.isPaused()) {
                    setPaused(false);
                }
            }
        });
        IBpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!musicService.isPaused()) {
                    setPaused(true);
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

        IBcontrolMenu = (ImageButton) findViewById(R.id.bottom_sheet_control_menu);
        IBcontrolMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
                popupMenu.inflate(R.menu.control_menu);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_copy:
                                try {
                                    ClipData clip = ClipData.newPlainText("SimpleMusic_SongTitle", TVsongTitle.getText());
                                    clipboardManager.setPrimaryClip(clip);
                                    Snackbar.make(rootView, "Скопiйовано", Snackbar.LENGTH_SHORT).show();
                                } catch (Exception ex) {
                                    Log.e(TAG, "PopupMenu -> OnMenuItemClickListener() -> ", ex);
                                    Snackbar.make(rootView, "Error occurred during copying", Snackbar.LENGTH_SHORT).show();
                                }
                                break;
                            case R.id.chrome_client:
                                CustomTabsIntent.Builder customTabsIntentBuilder = new CustomTabsIntent.Builder();

                                customTabsIntentBuilder.setToolbarColor(ContextCompat.getColor(MainActivity.this, R.color.colorAppPrimary));
                                customTabsIntentBuilder.setCloseButtonIcon(
                                        BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back_white_24dp));

                                CustomTabsIntent customTabsIntent = customTabsIntentBuilder.build();

                                CustomTabActivityHelper.openCustomTab(MainActivity.this, customTabsIntent,
                                        Uri.parse("https://en.wikipedia.org/wiki/" + TVsongSubtitle.getText().toString().replace(" ", "_")),
                                        new CustomTabActivityHelper.CustomTabFallback() {
                                            @Override
                                            public void openUri(Activity activity, Uri uri) {
                                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                                startActivity(intent);
                                            }
                                        });

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
                VolumeBottomSheetDialogFragment volumeDialog = new VolumeBottomSheetDialogFragment();
                volumeDialog.show(getSupportFragmentManager(), "VolumeDialog");
            }
        });
        IBcontrolVolume.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                muteAudio();
                return false;
            }
        });
    }

    /**
     * Used for handling changing of state of pause
     * <p/>
     * <p>
     * This method also changes UI elements in response on pause/unpause playback
     * Use this method instead of {@link MusicService#setPaused(boolean)}
     * </p>
     *
     * @param paused - State of pause
     */
    private void setPaused(boolean paused) {
        if (paused) {
            musicService.setPaused(true);
            IBplayStopBar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_primary_24dp));
        } else {
            musicService.setPaused(false);
            IBplayStopBar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_primary_24dp));
        }
    }

    /**
     * Playback method for skipping to next track
     * <p>
     * - Calls {@link MusicService#playNext()} for notifying service (e.g user's click on skipping button)
     * - Calls {@link MainActivity#startUpdateTask(Song)} for tracking state of playback
     * </p>
     */
    private void playNext() {
        musicService.playNext();
        startUpdateTask(songs.get(musicService.getSongPosition()));
    }

    /**
     * Playback method for skipping to previous track
     * <p>
     * - Calls {@link MusicService#playPrevious()} ()} for notifying service (e.g user's click on skipping button)
     * - Calls {@link MainActivity#startUpdateTask(Song)} for tracking state of playback
     * </p>
     */
    private void playPrevious() {
        musicService.playPrevious();
        startUpdateTask(songs.get(musicService.getSongPosition()));
    }

    /**
     * Implementation of {@code bottomSheetBehavior}'s logic
     * <p/>
     * <p>
     * You can implement reaction on different {@link BottomSheetBehavior} states
     * {@link BottomSheetBehavior#STATE_COLLAPSED} or sliding offset using
     * {@link BottomSheetBehavior#setBottomSheetCallback(BottomSheetBehavior.BottomSheetCallback)}
     * </p>
     */
    private void initBottomSheet() {
        // Bottom Sheet Primary
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

    /**
     * Implementation of {@code recyclerView}'s and {@code songAdapter} logic and passing new data to {@code songAdapter}
     */
    private void setUpRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        songs = new ArrayList<>();
        layoutManager = new LinearLayoutManager(this);
        songAdapter = new RecyclerViewAdapter(this, songs);

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

        setUpArrayList();
    }

    /**
     * Retrieve and sort ArrayList of songs using {@link MusicRetriever}
     * <p/>
     * <p>
     * Songs are retrieved from both internal and external storages.
     * For retrieving songs from internal and external storages separately use
     * {@link MusicRetriever#getSongFromExternalStorage()} or {@link MusicRetriever#getSongFromInternalStorage()} instead
     * </p>
     */
    private void setUpArrayList() {
        songs.addAll(musicRetriever.getSongFromExternalStorage());
        Log.d(TAG, "setUpArrayList() -> ArrayList size: " + songs.size());
        musicRetriever.sortListAlphabet(songs);
        songAdapter.notifyDataSetChanged();
    }

    /**
     * Method used for shuffling ArrayList of Song
     * <p/>
     * <p>
     * - Shuffle {@link ArrayList} of {@link Song} using {@link MusicRetriever}
     * - Notify {@code songAdapter} about changing of DataSet
     * with {@link RecyclerView.Adapter#notifyDataSetChanged()}
     * - Pass new songs ArrayList to {@link MusicService}
     * </p>
     */
    private void shuffleSongList() {
        musicRetriever.shuffleList(songs);
        songAdapter.notifyDataSetChanged();
        musicService.setSongs(songs);
    }

    /**
     * Method for starting {@link TimerTask} which will change UI in response of changing playback's state
     * <p/>
     * <p>
     * Method updates UI element such as {@link TextView}, {@link SeekBar} in response of playing
     * using {@link TimerTask} and {@link Timer}
     * </p>
     *
     * @param song - Current {@link Song}
     */
    private void startUpdateTask(Song song) {
        setPaused(false);

        stopUpdateTask();
        prepareUpdateTask(song);

        updateTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            PBsongBar.setProgress(musicService.getCurrentPosition());
                            TVtimeStampCurrent.setText(msToString(musicService.getCurrentPosition()));

                            Log.d(TAG, "updateTask -> Current position: " + msToString(musicService.getCurrentPosition()));
                        } catch (Exception ex) {
                            Log.e(TAG, "updateTask -> ", ex);
                        }
                    }
                });
            }
        };

        timer = new Timer();
        timer.schedule(updateTask, 0, 1000);
    }

    private void refreshUpdateTask(Song song) {
        setPaused(true);

        stopUpdateTask();
        prepareUpdateTask(song);

        updateTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            PBsongBar.setProgress(musicService.getCurrentPosition());
                            TVtimeStampCurrent.setText(msToString(musicService.getCurrentPosition()));

                            Log.d(TAG, "updateTask -> Current position: " + msToString(musicService.getCurrentPosition()));
                        } catch (Exception ex) {
                            Log.e(TAG, "updateTask -> ", ex);
                        }
                    }
                });
            }
        };

        timer = new Timer();
        timer.schedule(updateTask, 0, 1000);
    }

    /**
     * Check and stop {@link MainActivity#updateTask} and {@link MainActivity#timer}
     */
    private void stopUpdateTask() {
        if (timer != null)
            timer.cancel();
        if (updateTask != null)
            updateTask.cancel();
    }

    /**
     * Prepare update task
     * {@link MainActivity#startUpdateTask(Song)}
     *
     * @param song - Current {@link Song}
     */
    private void prepareUpdateTask(Song song) {
        Log.d(TAG, "prepareUpdateTask() ->" +
                "\nArtist: " + song.getArtist() +
                "\nTitle: " + song.getTitle() +
                "\nAlbum art Uri: " + song.getAlbumArt()
        );

        // Bottom Bar
        IValbumArtBar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        try {
            if (song.getAlbumArt().equalsIgnoreCase("")) {
                IValbumArtBar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_music_note_primary_100_24dp));
            } else {
                /***********************SAFE LOADING WITH PICASSO (instead of Glide)***********************/
                IValbumArtBar.setScaleType(ImageView.ScaleType.FIT_CENTER);
                Picasso
                        .with(this)
                        .load(song.getAlbumArt())
                        .into(IValbumArtBar, new Callback() {
                            @Override
                            public void onSuccess() {
                                if (IValbumArtBar.getDrawable() == null) {
                                    Log.e(TAG, "prepareUpdateTask() -> onSuccess() -> Drawable is null");

                                    IValbumArtBar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                    Picasso
                                            .with(MainActivity.this)
                                            .load(R.drawable.ic_music_note_primary_100_24dp)
                                            .into(IValbumArtBar);
                                }
                            }

                            @Override
                            public void onError() {
                                Log.e(TAG, "prepareUpdateTask() -> onError() -> ");

                                IValbumArtBar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                Picasso
                                        .with(MainActivity.this)
                                        .load(R.drawable.ic_music_note_primary_100_24dp)
                                        .into(IValbumArtBar);
                            }
                        });
                /********************************************************************************************/
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            IValbumArtBar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_music_note_primary_100_24dp));
        }

        PBsongBar.setProgress(0);
        PBsongBar.setMax(musicService.getDuration());

        TVsongTitleBar.setText(song.getTitle());
        TVsongSubtitleBar.setText(song.getArtist());

        // Bottom Sheet

        TVsongTitle.setText(song.getTitle());
        TVsongSubtitle.setText(song.getArtist());

        TVtimeStampDuration.setText(msToString(musicService.getDuration()));
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

    /**
     * Method for toggling repeat state
     */
    private void toggleRepeatState() {
        if (musicService.getRepeatState()) {
            setRepeatOff();
        } else {
            setRepeatOn();
        }
    }

    /**
     * {@link MainActivity#toggleRepeatState()} ()}
     */
    private void setRepeatOn() {
        musicService.setRepeatOn();
        IBcontrolRepeat.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_primary_24dp));
    }

    /**
     * {@link MainActivity#toggleRepeatState()} ()}
     */
    private void setRepeatOff() {
        musicService.setRepeatOff();
        IBcontrolRepeat.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_black_24dp));
    }

    /**
     * Method for toggling state of shuffle
     */
    public void toggleShuffleState() {
        if (musicService.getShuffleState()) {
            setShuffleOff();
        } else {
            setShuffleOn();
        }
    }

    /**
     * {@link MainActivity#toggleShuffleState()}
     */
    private void setShuffleOn() {
        musicService.setShuffleOn();
        IBcontrolShuffle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_shuffle_primary_24dp));
    }

    /**
     * {@link MainActivity#toggleShuffleState()}
     */
    private void setShuffleOff() {
        musicService.setShuffleOff();
        IBcontrolShuffle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_shuffle_black_24dp));
    }

    private void checkMute() {
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_volume_off_black_24dp));
        } else {
            IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_volume_up_black_24dp));
        }
    }

    private void muteAudio() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        checkMute();
    }

    public static class VolumeBottomSheetDialogFragment extends BottomSheetDialogFragment {
        private final String TAG = "VolumeDialog";

        private Activity activity;


        private ImageButton IBVolumeMusic;
        private AppCompatSeekBar SBVolumeMusic;

        private ImageButton IBVolumePhone;
        private AppCompatSeekBar SBVolumePhone;

        private boolean isMutedMusic = false;
        private boolean isMutedPhone = false;

        private int musicVolume;
        private int phoneVolume;

        @Override
        public void setupDialog(Dialog dialog, int style) {
            activity = getActivity();
            View mainView = activity.getLayoutInflater().inflate(R.layout.dialog_volume, null);
            dialog.setContentView(mainView);

            IBVolumeMusic = (ImageButton) mainView.findViewById(R.id.image_button_volume_music);
            SBVolumeMusic = (AppCompatSeekBar) mainView.findViewById(R.id.seek_bar_volume_music);

            IBVolumePhone = (ImageButton) mainView.findViewById(R.id.image_button_volume_phone);
            SBVolumePhone = (AppCompatSeekBar) mainView.findViewById(R.id.seek_bar_volume_phone);

            refreshSeekBars();
            checkMute();

            // Music block
            IBVolumeMusic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleMuteMusic();
                }
            });
            SBVolumeMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    Log.d(TAG, "Volume SeekBar: " + progress);

                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);

                    if (progress == 0) {
                        MainActivity.IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_volume_off_black_24dp));
                        IBVolumeMusic.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_volume_off_black_24dp));
                    } else {
                        MainActivity.IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_volume_up_black_24dp));
                        IBVolumeMusic.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_volume_up_black_24dp));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            // Phone block
            IBVolumePhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleMutePhone();
                }
            });
            SBVolumePhone.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    Log.d(TAG, "Volume SeekBar: " + progress);

                    audioManager.setStreamVolume(AudioManager.STREAM_RING, progress, 0);

                    if (progress == 0) {
                        IBVolumePhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_vibration_black_24dp));
                    } else {
                        IBVolumePhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_phonelink_ring_black_24dp));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            super.setupDialog(dialog, style);
        }

        private void checkMute() {
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                MainActivity.IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_volume_off_black_24dp));

                IBVolumeMusic.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_volume_off_black_24dp));
                SBVolumeMusic.setProgress(0);
            } else {
                MainActivity.IBcontrolVolume.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_volume_up_black_24dp));

                IBVolumeMusic.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_volume_up_black_24dp));
            }

            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                IBVolumePhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_vibration_black_24dp));
                SBVolumePhone.setProgress(0);
            } else {
                IBVolumePhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_phonelink_ring_black_24dp));
            }
        }

        private void refreshSeekBars() {
            SBVolumeMusic.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            SBVolumeMusic.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

            SBVolumePhone.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
            SBVolumePhone.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_RING));
        }

        private void toggleMuteMusic() {
            if (isMutedMusic) {
                isMutedMusic = false;
                unmuteMusic();
            } else {
                isMutedMusic = true;
                muteMusic();
            }
        }

        private void toggleMutePhone() {
            if (isMutedPhone) {
                isMutedPhone = false;
                unmutePhone();
            } else {
                isMutedPhone = true;
                mutePhone();
            }
        }

        private void muteMusic() {
            musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            checkMute();
        }

        private void unmuteMusic() {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolume, 0);
            refreshSeekBars();
        }

        private void mutePhone() {
            phoneVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            checkMute();
        }

        private void unmutePhone() {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, phoneVolume, 0);
            refreshSeekBars();
        }
    }

    /**
     * BroadcastReceiver for handling headset plug/unplug
     */
    private class MusicIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        if (wasPlugged) {
                            setPaused(true);
                        }
                        //Snackbar.make(rootView, "Headset is unplugged", Snackbar.LENGTH_SHORT).show();
                        break;
                    case 1:
                        wasPlugged = true;
                        //Snackbar.make(rootView, "Headset is plugged", Snackbar.LENGTH_SHORT).show();
                        break;
                    default:
                        //Snackbar.make(rootView, "I have no idea what the headset state is", Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

}