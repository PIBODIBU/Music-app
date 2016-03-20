package com.android.simplemusic;


import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private final String TAG = "RecyclerViewAdapter";

    private Context context;
    private ArrayList<Song> dataSet;
    private RecyclerViewCallbacks recyclerViewCallbacks;

    public RecyclerViewAdapter(Context context, ArrayList<Song> data) {
        this.context = context;
        this.dataSet = data;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout songLay;
        TextView songView;
        TextView artistView;
        ImageView albumArt;

        public MyViewHolder(View itemView) {
            super(itemView);
            this.songLay = (LinearLayout) itemView.findViewById(R.id.layout);
            this.songView = (TextView) itemView.findViewById(R.id.song_title);
            this.artistView = (TextView) itemView.findViewById(R.id.song_artist);
            this.albumArt = (ImageView) itemView.findViewById(R.id.album_art);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        LinearLayout songLay = holder.songLay;
        TextView songView = holder.songView;
        TextView artistView = holder.artistView;
        final ImageView albumArt = holder.albumArt;

        Song currSong = dataSet.get(position);

        if (albumArt != null) {
            if (!currSong.getAlbumArt().equalsIgnoreCase("")) {
                Picasso
                        .with(context)
                        .load(currSong.getAlbumArt())
                        .into(albumArt, new Callback() {
                            @Override
                            public void onSuccess() {
                                if (albumArt.getDrawable() == null) {
                                    Log.e(TAG, "onBindViewHolder() -> onSuccess() -> Drawable is null");
                                    Picasso
                                            .with(context)
                                            .load(R.drawable.ic_music_note_primary_100_24dp)
                                            .into(albumArt);
                                }
                            }

                            @Override
                            public void onError() {
                                Log.e(TAG, "onBindViewHolder() -> onError() -> ");
                                Picasso
                                        .with(context)
                                        .load(R.drawable.ic_music_note_primary_100_24dp)
                                        .into(albumArt);
                            }
                        });
            } else {
                Glide
                        .with(context)
                        .load(R.drawable.ic_music_note_primary_100_24dp)
                        .crossFade()
                        .into(albumArt);
            }
        }

        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());

        songLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recyclerViewCallbacks != null) {
                    recyclerViewCallbacks.songPicked(holder.getAdapterPosition());
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public void setRecyclerViewCallbacks(RecyclerViewCallbacks recyclerViewCallbacks) {
        this.recyclerViewCallbacks = recyclerViewCallbacks;
    }

    public interface RecyclerViewCallbacks {
        void songPicked(int position);
    }
}