package com.example.do_an.presentation.reading.reader.adapter;

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
import com.github.chrisbanes.photoview.PhotoView;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder> {

    private static final String TAG = "PdfPageAdapter";
    private Context context;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;
    private Bitmap[] bitmapCache;
    public int pageMode = 1;

    public void setPageMode(int mode) {
        if (mode < 1)
            mode = 1;
        this.pageMode = mode;
    }

    public PdfPageAdapter(Context context, File pdfFile) {
        this.context = context;
        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
        } catch (IOException e) {
            Log.e(TAG, "Không thể mở file PDF", e);
        }
    }

    @NonNull
    @Override
    public PdfPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.pdf_list_item_pdf_page, parent, false);
        return new PdfPageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfPageViewHolder holder, int position) {
        if (pdfRenderer == null)
            return;

        if (bitmapCache == null)
            bitmapCache = new Bitmap[getItemCount()];

        if (bitmapCache[position] != null) {
            holder.pageImageView.setImageBitmap(bitmapCache[position]);
            return;
        }

        Bitmap bitmap = null;

        if (pageMode == 1) {
            bitmap = renderSinglePage(position);
        } else {
            bitmap = renderDoublePage(position);
        }

        if (bitmap != null) {
            holder.pageImageView.setImageBitmap(bitmap);
            bitmapCache[position] = bitmap; // lưu vào cache
        }
    }

    private Bitmap renderSinglePage(int pageIndex) {
        PdfRenderer.Page page = null;
        try {
            page = pdfRenderer.openPage(pageIndex);
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi render trang " + pageIndex, e);
            return null;
        } finally {
            if (page != null)
                page.close();
        }
    }

    private Bitmap renderDoublePage(int position) {
        int total = pdfRenderer.getPageCount();
        int leftIndex = position * 2;
        int rightIndex = leftIndex + 1;

        PdfRenderer.Page leftPage = null;
        PdfRenderer.Page rightPage = null;

        try {
            leftPage = pdfRenderer.openPage(leftIndex);
            int width = leftPage.getWidth();
            int height = leftPage.getHeight();

            if (rightIndex < total) {
                rightPage = pdfRenderer.openPage(rightIndex);
                width += rightPage.getWidth();
                height = Math.max(height, rightPage.getHeight());
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            Bitmap leftBitmap = Bitmap.createBitmap(leftPage.getWidth(), leftPage.getHeight(), Bitmap.Config.ARGB_8888);
            leftPage.render(leftBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            canvas.drawBitmap(leftBitmap, 0, 0, null);
            leftBitmap.recycle();

            if (rightPage != null) {
                Bitmap rightBitmap = Bitmap.createBitmap(rightPage.getWidth(), rightPage.getHeight(),
                        Bitmap.Config.ARGB_8888);
                rightPage.render(rightBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                canvas.drawBitmap(rightBitmap, leftPage.getWidth(), 0, null);
                rightBitmap.recycle();
            }

            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Lỗi render trang đôi", e);
            return null;
        } finally {
            if (leftPage != null)
                leftPage.close();
            if (rightPage != null)
                rightPage.close();
        }
    }

    public Bitmap getPageBitmap(int pageIndex) {
        if (bitmapCache != null && bitmapCache[pageIndex] != null) {
            return bitmapCache[pageIndex];
        }
        return renderSinglePage(pageIndex);
    }

    private void renderPage(ImageView imageView, int pageIndex) {
        PdfRenderer.Page page = null;
        try {
            page = pdfRenderer.openPage(pageIndex);
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi render trang " + pageIndex, e);
        } finally {
            if (page != null)
                page.close();
        }
    }

    @Override
    public int getItemCount() {
        if (pdfRenderer == null)
            return 0;
        int total = pdfRenderer.getPageCount();
        if (pageMode == 1)
            return total;
        // mỗi item hiển thị 2 trang
        return (total + 1) / 2; // ceil(total / 2)
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

    static class PdfPageViewHolder extends RecyclerView.ViewHolder {
        PhotoView pageImageView;

        public PdfPageViewHolder(@NonNull View itemView) {
            super(itemView);
            pageImageView = itemView.findViewById(R.id.pageImageView);

            pageImageView.setMinimumScale(1f);
            pageImageView.setMaximumScale(5f);
            pageImageView.setAllowParentInterceptOnEdge(true);

        }
    }

}