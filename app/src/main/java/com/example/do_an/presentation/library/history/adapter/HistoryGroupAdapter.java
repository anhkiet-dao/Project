package com.example.do_an.presentation.library.history.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.domain.library.model.HistoryGroup;

import java.util.ArrayList;

public class HistoryGroupAdapter extends RecyclerView.Adapter<HistoryGroupAdapter.ViewHolder> {

    private final ArrayList<HistoryGroup> list;

    public HistoryGroupAdapter(ArrayList<HistoryGroup> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item_history_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryGroup group = list.get(position);
        holder.textDate.setText(group.getDate());

        HistoryItemAdapter innerAdapter = new HistoryItemAdapter(group.getItems());
        holder.recyclerInner.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerInner.setAdapter(innerAdapter);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDate;
        RecyclerView recyclerInner;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.tvDate);
            recyclerInner = itemView.findViewById(R.id.recyclerInner);
        }
    }
}
