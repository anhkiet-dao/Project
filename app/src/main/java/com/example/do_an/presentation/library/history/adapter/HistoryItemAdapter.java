package com.example.do_an.presentation.library.history.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.domain.library.model.HistoryItem;

import java.util.ArrayList;

public class HistoryItemAdapter extends RecyclerView.Adapter<HistoryItemAdapter.ViewHolder> {

    private final ArrayList<HistoryItem> list;

    public HistoryItemAdapter(ArrayList<HistoryItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = list.get(position);

        holder.textTitle.setText(item.getTitle());
        holder.textAuthor.setText(holder.itemView.getContext().getString(R.string.history_author, item.getAuthor()));
        holder.textTime
                .setText(holder.itemView.getContext().getString(R.string.history_start_time, item.getStartTime()));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_loading)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imageCover);
        } else {
            holder.imageCover.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textAuthor, textTime;
        ImageView imageCover;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.tvTitle);
            textAuthor = itemView.findViewById(R.id.tvAuthor);
            textTime = itemView.findViewById(R.id.tvTime);
            imageCover = itemView.findViewById(R.id.ivCoverImage);
        }
    }
}
