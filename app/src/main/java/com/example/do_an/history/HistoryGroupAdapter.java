package com.example.do_an.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

public class HistoryGroupAdapter extends ListAdapter<HistoryGroup, HistoryGroupAdapter.HistoryGroupViewHolder> {

    public HistoryGroupAdapter() {
        super(new HistoryGroupDiffCallback());
    }

    @NonNull
    @Override
    public HistoryGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item_history_group, parent, false);
        return new HistoryGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryGroupViewHolder holder, int position) {
        HistoryGroup group = getItem(position);
        holder.tvDate.setText(group.date);

        // Reuse inner adapter and submit the new list
        holder.innerAdapter.submitList(group.items);
    }

    public static class HistoryGroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        RecyclerView recyclerInner;
        HistoryItemAdapter innerAdapter;

        public HistoryGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            recyclerInner = itemView.findViewById(R.id.recyclerInner);

            recyclerInner.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            innerAdapter = new HistoryItemAdapter();
            recyclerInner.setAdapter(innerAdapter);
        }
    }
}