package com.example.do_an.presentation.library.home.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.domain.library.model.Book;

import java.util.List;

public class BookHomeAdapter extends RecyclerView.Adapter<BookHomeAdapter.ViewHolder> {

    public interface OnBookClickListener {
        void onClick(Book book);
    }

    private final Context context;
    private final List<Book> list;
    private final OnBookClickListener listener;

    public BookHomeAdapter(Context context, List<Book> list, OnBookClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_item_book2, parent, false);
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

        holder.txtName.setText(book.getName() != null ? book.getName() : "Chưa cập nhật");

        holder.itemView.setOnClickListener(v -> listener.onClick(book));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txtName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_book);
            txtName = itemView.findViewById(R.id.txtName);
        }
    }
}
