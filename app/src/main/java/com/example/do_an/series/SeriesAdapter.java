package com.example.do_an.series;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

import java.util.Objects;

public class SeriesAdapter extends ListAdapter<Series, SeriesAdapter.ViewHolder> {

    private OnSeriesClick listener = series -> {};

    public interface OnSeriesClick {
        void onClick(Series series);
    }

    public void setOnSeriesClickListener(OnSeriesClick listener) {
        this.listener = listener;
    }

    public SeriesAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Series> DIFF_CALLBACK = new DiffUtil.ItemCallback<Series>() {
        @Override
        public boolean areItemsTheSame(@NonNull Series oldItem, @NonNull Series newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Series oldItem, @NonNull Series newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName())
                    && Objects.equals(oldItem.getLink(), newItem.getLink());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.series_item_series, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Series series = getItem(position);
        String name = series.getName();

        String display = name != null
                ? name.replace("Tap ", "Tập ").replace("tap ", "Tập ")
                : "Tập " + (position + 1);

        holder.tvName.setText(display);
        holder.itemView.setOnClickListener(v -> listener.onClick(series));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.txtSeriesName);
        }
    }
}