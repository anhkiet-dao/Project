package com.example.do_an.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

import java.io.File;
import java.io.IOException;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder> {

    private static final String TAG = "PdfPageAdapter";
    private Context context;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    public PdfPageAdapter(Context context, File pdfFile) {
        this.context = context;
        try {
            // PdfRenderer yêu cầu một ParcelFileDescriptor
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
        } catch (IOException e) {
            Log.e(TAG, "Không thể mở file PDF", e);
        }
    }

    @NonNull
    @Override
    public PdfPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng cái "khuôn" list_item_pdf_page.xml
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_pdf_page, parent, false);
        return new PdfPageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfPageViewHolder holder, int position) {
        if (pdfRenderer == null) {
            return;
        }

        PdfRenderer.Page currentPage = null;
        try {
            // 1. Mở đúng trang
            currentPage = pdfRenderer.openPage(position);

            // 2. Tạo Bitmap rỗng
            Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

            // 3. Vẽ trang PDF lên Bitmap
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            // 4. Đặt Bitmap cho ImageView
            holder.pageImageView.setImageBitmap(bitmap);

        } catch (Exception e) {
            Log.e(TAG, "Lỗi render trang " + position, e);
        } finally {
            // 5. Luôn đóng trang lại để giải phóng bộ nhớ
            if (currentPage != null) {
                currentPage.close();
            }
        }
    }

    @Override
    public int getItemCount() {
        // Trả về tổng số trang
        return (pdfRenderer != null) ? pdfRenderer.getPageCount() : 0;
    }

    // Rất quan trọng: Dọn dẹp
    public void close() {
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi đóng PdfRenderer", e);
        }
    }

    // ViewHolder để giữ ImageView
    static class PdfPageViewHolder extends RecyclerView.ViewHolder {
        ImageView pageImageView;

        public PdfPageViewHolder(@NonNull View itemView) {
            super(itemView);
            pageImageView = itemView.findViewById(R.id.pageImageView);
        }
    }
}