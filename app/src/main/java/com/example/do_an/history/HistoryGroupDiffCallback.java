package com.example.do_an.history;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

class HistoryGroupDiffCallback extends DiffUtil.ItemCallback<HistoryGroup> {
    @Override
    public boolean areItemsTheSame(@NonNull HistoryGroup oldItem, @NonNull HistoryGroup newItem) {
        return oldItem.date.equals(newItem.date);
    }

    @Override
    public boolean areContentsTheSame(@NonNull HistoryGroup oldItem, @NonNull HistoryGroup newItem) {
        if (oldItem.items.size() != newItem.items.size()) {
            return false;
        }
        for (int i = 0; i < oldItem.items.size(); i++) {
            if (!Objects.equals(
                    oldItem.items.get(i).endTime,
                    newItem.items.get(i).endTime)) {

                return false;
            }
        }
        return true;
    }
}
