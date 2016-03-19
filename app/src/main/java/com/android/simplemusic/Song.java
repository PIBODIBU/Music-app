package com.android.simplemusic;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String albumArt;

    public Song(long songID, String songTitle, String songArtist, String albumArt) {
        id = songID;
        title = songTitle;
        artist = songArtist;
        this.albumArt = albumArt;
    }

    public long getID() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumArt() {
        return albumArt;
    }
}
