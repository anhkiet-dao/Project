package com.example.do_an.presentation.library.series.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.do_an.R;
import com.example.do_an.domain.library.model.Series;

import java.util.List;

public class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.ViewHolder> {

    private final List<Series> list;
    private final OnSeriesClick listener;

    public interface OnSeriesClick { void onClick(Series series); }

    public SeriesAdapter(List<Series> list, OnSeriesClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.series_item_series, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Series series = list.get(position);
        String name = series.getName();

        String display = name != null ? name.replace("Tap ", "Tập ").replace("tap ", "Tập ") : "Tập " + (position + 1);

        holder.txtName.setText(display);
        holder.itemView.setOnClickListener(v -> listener.onClick(series));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtSeriesName);
        }
    }
}