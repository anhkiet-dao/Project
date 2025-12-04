package com.example.do_an.history;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

class HistoryItemDiffCallback extends DiffUtil.ItemCallback<HistoryItem> {
    @Override
    public boolean areItemsTheSame(@NonNull HistoryItem oldItem, @NonNull HistoryItem newItem) {
        // If there is no unique id, compare title+startTime as identity
        return (oldItem.title + "|" + oldItem.startTime).equals(newItem.title + "|" + newItem.startTime);
    }

    @Override
    public boolean areContentsTheSame(@NonNull HistoryItem oldItem, @NonNull HistoryItem newItem) {
        return oldItem.title.equals(newItem.title)
                && oldItem.author.equals(newItem.author)
                && oldItem.startTime.equals(newItem.startTime)
                && oldItem.endTime.equals(newItem.endTime);
    }
}

