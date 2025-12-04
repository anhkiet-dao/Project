package com.example.do_an.story;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

class StoryDiffCallback extends DiffUtil.ItemCallback<Story> {
    @Override
    public boolean areItemsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
        // Use document ID from Firestore - should always be set
        return Objects.equals(oldItem.getId(), newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
        return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                && Objects.equals(oldItem.getAuthor(), newItem.getAuthor())
                && Objects.equals(oldItem.getYear(), newItem.getYear())
                && Objects.equals(oldItem.getGenre(), newItem.getGenre())
                && Objects.equals(oldItem.getThumbnail(), newItem.getThumbnail());
    }
}
