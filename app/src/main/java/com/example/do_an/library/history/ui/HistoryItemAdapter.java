package com.example.do_an.library.history.item;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.library.history.model.HistoryItem;

public class HistoryItemAdapter extends ListAdapter<HistoryItem, HistoryItemAdapter.ViewHolder> {

    public HistoryItemAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<HistoryItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<HistoryItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull HistoryItem oldItem, @NonNull HistoryItem newItem) {
            return (oldItem.title + "|" + oldItem.startTime).equals(newItem.title + "|" + newItem.startTime);
        }

        @Override
        public boolean areContentsTheSame(@NonNull HistoryItem oldItem, @NonNull HistoryItem newItem) {
            return oldItem.title.equals(newItem.title)
                    && oldItem.author.equals(newItem.author)
                    && oldItem.startTime.equals(newItem.startTime)
                    && oldItem.endTime.equals(newItem.endTime);
        }
    };

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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitleAuthor, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitleAuthor = itemView.findViewById(R.id.tvTitleAuthor);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
