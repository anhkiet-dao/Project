package com.example.do_an.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView; // Import CardView
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
    private String selectedBookId = null; // Biến mới để lưu ID cuốn sách được chọn

    public BookImageAdapter(Context context, List<Book> list, OnBookClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    // Phương thức mới để cập nhật ID sách được chọn
    public void setSelectedBookId(String bookId) {
        this.selectedBookId = bookId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // *Chú ý: Để sử dụng hiệu ứng nổi bật CardView, R.layout.home_item_book
        // cần phải là CardView hoặc chứa một CardView có ID để truy cập
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_item_book, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Book book = list.get(position);

        // --- LOGIC HIGHLIGHT ---
        boolean isSelected = selectedBookId != null && selectedBookId.equals(book.getId());

        // Giả định itemView của adapter này là CardView (hoặc có CardView trong ViewHolder)
        // Nếu layout item là CardView, ta có thể cast trực tiếp.
        if (holder.itemView instanceof CardView) {
            CardView cardView = (CardView) holder.itemView;
            if (isSelected) {
                // Áp dụng hiệu ứng nổi bật (ví dụ: đường viền màu và đổ bóng lớn hơn)
                cardView.setCardElevation(dpToPx(context, 8)); // Tăng đổ bóng

                // *Lưu ý: setStrokeColor/setStrokeWidth chỉ dùng được cho MaterialCardView
                // Nếu bạn dùng CardView thông thường, chỉ có thể thay đổi CardElevation hoặc Background Color
                // Ở đây, tôi sẽ dùng setCardElevation và đặt màu nền để mô phỏng.
                cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0")); // Màu nền nhẹ

            } else {
                // Trả về mặc định
                cardView.setCardElevation(dpToPx(context, 2)); // Đổ bóng mặc định
                cardView.setCardBackgroundColor(Color.WHITE); // Màu nền mặc định
            }
        } else {
            // Nếu không phải CardView, ta dùng Background Resource
            if (isSelected) {
                holder.itemView.setBackgroundResource(R.drawable.highlight_border); // Giả định bạn đã tạo resource này
            } else {
                holder.itemView.setBackgroundResource(0); // Không có nền
            }
        }
        // --- KẾT THÚC LOGIC HIGHLIGHT ---

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

    // Hàm chuyển đổi DP sang PX
    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_book);
            // Nếu bạn muốn truy cập CardView bên trong, bạn cần tìm nó ở đây
        }
    }
}