package com.example.do_an.presentation.library.home.adapter; // Đặt cùng package với BookHomeAdapter

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

public class AllBooksAdapter extends RecyclerView.Adapter<AllBooksAdapter.BookViewHolder> {

    private final Context context;
    private final List<Book> bookList;
    private final BookClickListener bookClickListener;

    public interface BookClickListener {
        void onBookClick(Book book);
    }

    public AllBooksAdapter(Context context, List<Book> bookList, BookClickListener listener) {
        this.context = context;
        this.bookList = bookList;
        this.bookClickListener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.home_item_book_grid, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);

        holder.txtTitle.setText(book.getName());

        Glide.with(context)
                .load(book.getImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            if (bookClickListener != null) {
                bookClickListener.onBookClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtTitle;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_book_cover);
            txtTitle = itemView.findViewById(R.id.txt_book_title);
        }
    }
}