package com.example.do_an.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

public class HistoryItemAdapter extends ListAdapter<HistoryItem, HistoryItemAdapter.ViewHolder> {

    public HistoryItemAdapter() {
        super(new HistoryItemDiffCallback());
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
        HistoryItem item = getItem(position);
        holder.tvTitleAuthor.setText(item.title + " - " + item.author);
        holder.tvTime.setText("Bắt đầu: " + item.startTime + "\nKết thúc: " + item.endTime);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitleAuthor, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitleAuthor = itemView.findViewById(R.id.tvTitleAuthor);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
