package com.example.do_an.history.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.history.item.HistoryItemAdapter;

import java.util.ArrayList;
import java.util.Objects;

public class HistoryGroupAdapter extends ListAdapter<HistoryGroup, HistoryGroupAdapter.ViewHolder> {

    private final RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

    public HistoryGroupAdapter() {
        super(DIFF_CALLBACK);
    }

    public static final DiffUtil.ItemCallback<HistoryGroup> DIFF_CALLBACK = new DiffUtil.ItemCallback<HistoryGroup>() {
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
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item_history_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryGroup group = getItem(position);
        holder.tvDate.setText(group.date);

        if (holder.innerAdapter == null) {
            holder.innerAdapter = new HistoryItemAdapter();
            holder.recyclerInner.setAdapter(holder.innerAdapter);
            holder.recyclerInner.setRecycledViewPool(sharedPool);
            holder.recyclerInner.setNestedScrollingEnabled(false);
        }

        holder.innerAdapter.submitList(new ArrayList<>(group.items));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        RecyclerView recyclerInner;
        HistoryItemAdapter innerAdapter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDate = itemView.findViewById(R.id.tvDate);
            recyclerInner = itemView.findViewById(R.id.recyclerInner);
            recyclerInner.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }
}