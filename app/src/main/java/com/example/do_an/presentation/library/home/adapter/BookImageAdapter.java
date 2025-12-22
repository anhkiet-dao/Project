package com.example.do_an.presentation.library.home.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.domain.library.model.Book;

import java.util.List;

public class BookImageAdapter extends RecyclerView.Adapter<BookImageAdapter.ViewHolder> {

    public interface OnBookClickListener {
        void onClick(Book book);
    }

    private final Context context;
    private final List<Book> list;
    private final OnBookClickListener listener;
    private String selectedBookId = null;

    public BookImageAdapter(Context context, List<Book> list, OnBookClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    public void setSelectedBookId(String bookId) {
        this.selectedBookId = bookId;
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

        boolean isSelected = selectedBookId != null && selectedBookId.equals(book.getId());

        if (holder.itemView instanceof CardView) {
            CardView cardView = (CardView) holder.itemView;
            if (isSelected) {
                cardView.setCardElevation(dpToPx(context, 8));

                cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0"));

            } else {
                cardView.setCardElevation(dpToPx(context, 2));
                cardView.setCardBackgroundColor(Color.WHITE);
            }
        } else {
            if (isSelected) {
                holder.itemView.setBackgroundResource(R.drawable.highlight_border);
            } else {
                holder.itemView.setBackgroundResource(0);
            }
        }

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

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_book);
        }
    }
}