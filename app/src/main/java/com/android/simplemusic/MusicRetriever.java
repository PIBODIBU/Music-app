package com.android.simplemusic;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class MusicRetriever {

    private static final String TAG = "MusicRetriever";

    private Context context;

    public MusicRetriever(Context context) {
        this.context = context;
    }

    /**
     * Method used for retrieving music files from both external and internal storages
     *
     * @return - {@link ArrayList} of {@link Song}
     */
    public ArrayList<Song> getSongs() {
        ArrayList<Song> songs = getSongFromExternalStorage();
        songs.addAll(getSongFromInternalStorage());

        return songs;
    }

    /**
     * Method used for retrieving music files from external storage
     * <p/>
     * <p>
     * The most common method for retrieving music files from external storage.
     * Retrieving logic is based on {@link Cursor}. Songs are storaged in {@code ArrayList<Song>}.
     * {@link Song} is used as DataModel for saving song's data (e.g. artist name, album art uri, etc.).
     * </p>
     *
     * @return - {@link ArrayList} of {@link Song}
     */
    public ArrayList<Song> getSongFromExternalStorage() {
        ArrayList<Song> songs = new ArrayList<>();

        ContentResolver musicResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");

        Log.d(TAG, "getSongFromExternalStorage() -> EXTERNAL_CONTENT_URI: " + musicUri);

        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            // Getting indexes of columns
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumIdColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM_ID);

            // Retrieving data
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                long albumId = musicCursor.getLong(albumIdColumn);
                String albumArt = "";

                try {
                    albumArt = (ContentUris.withAppendedId(artworkUri, albumId)).toString();
                } catch (Exception ex) {
                    Log.e(TAG, "getSongFromExternalStorage() -> ", ex);
                }

                // Adding new Song to ArrayList
                songs.add(new Song(thisId, thisTitle, thisArtist, albumArt));
            }
            while (musicCursor.moveToNext());
        }

        if (musicCursor != null)
            musicCursor.close();

        Log.d(TAG, "getSongFromExternalStorage() -> ArrayList size:" + songs.size());

        return songs;
    }

    /**
     * Method used for retrieving music files from internal storage
     * <p/>
     * <p>
     * WARNING! Use it at your own risk.
     * This method retrieves all music files from internal storage (include system sounds).
     * Playing some ot them may cause Application crash.
     * </p>
     *
     * @return - {@link ArrayList} of {@link Song}
     */
    public ArrayList<Song> getSongFromInternalStorage() {
        ArrayList<Song> songs = new ArrayList<>();

        ContentResolver musicResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");

        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumIdColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM_ID);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                long albumId = musicCursor.getLong(albumIdColumn);
                String albumArt = "";

                try {
                    albumArt = (ContentUris.withAppendedId(artworkUri, albumId)).toString();
                } catch (Exception ex) {
                    Log.e(TAG, "getSongFromInternalStorage() -> ", ex);
                }

                songs.add(new Song(thisId, thisTitle, thisArtist, albumArt));
            }
            while (musicCursor.moveToNext());
        }

        if (musicCursor != null)
            musicCursor.close();

        Log.d(TAG, "getSongFromInternalStorage() -> ArrayList size:" + songs.size());

        return songs;
    }

    /**
     * Method for sorting {@link ArrayList} of {@link Song} in alphabet order
     *
     * @param songs - {@link ArrayList} for sorting
     */
    public void sortListAlphabet(ArrayList<Song> songs) {
        Collections.sort(songs, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    /**
     * Method for shuffling {@link ArrayList}
     *
     * @param arrayList - ArrayList for shuffling
     */
    public void shuffleList(ArrayList<Song> arrayList) {
        Collections.shuffle(arrayList, new Random(getRandomSeed()));
    }

    /**
     * Get random seed based on {@link System#currentTimeMillis()}
     *
     * @return - New random seed
     */
    private long getRandomSeed() {
        return System.currentTimeMillis();
    }
}
