package com.example.do_an.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;

import java.util.List;

public class BookImageAdapter extends RecyclerView.Adapter<BookImageAdapter.ViewHolder> {

    public interface OnBookClickListener {
        void onClick(Book book);
    }

    private final Context context;
    private final List<Book> list;
    private final OnBookClickListener listener;

    public BookImageAdapter(Context context, List<Book> list, OnBookClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_item_book, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Book book = list.get(position);

        Glide.with(context)
                .load(book.getImageUrl())
                .override(300, 400)
                .centerCrop()
                .into(holder.img);

        holder.itemView.setOnClickListener(v -> listener.onClick(book));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_book);
        }
    }
}
