package com.android.simplemusic;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private ArrayList<Song> dataSet;
    private RecyclerViewCallbacks recyclerViewCallbacks;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout songLay;
        TextView songView;
        TextView artistView;


        public MyViewHolder(View itemView) {
            super(itemView);
            this.songLay = (LinearLayout) itemView.findViewById(R.id.layout);
            this.songView = (TextView) itemView.findViewById(R.id.song_title);
            this.artistView = (TextView) itemView.findViewById(R.id.song_artist);
        }
    }

    public RecyclerViewAdapter(ArrayList<Song> data) {
        this.dataSet = data;
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

        Song currSong = dataSet.get(position);

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