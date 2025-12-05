package com.example.do_an.home;

import android.content.Context;
import androidx.viewpager2.widget.ViewPager2;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
// ⭐ KHẮC PHỤC LỖI CANNOT RESOLVE SYMBOL ⭐
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class PdfViewerUtility {

    private static final String TAG = "PdfViewerUtility";
    private final Context context;
    private final ViewPager2 viewPager;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;

    public PdfViewerUtility(Context context, ViewPager2 viewPager) {
        this.context = context;
        this.viewPager = viewPager;
    }

    // ⭐ Phương thức loadPdfPreview ⭐
    public void loadPdfPreview(String pdfUrl, int maxPages) {
        new DownloadAndRenderTask(maxPages).execute(pdfUrl);
    }

    // ⭐ Phương thức closeRenderer ⭐
    public void closeRenderer() {
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing PDF renderer or file descriptor", e);
        }
    }

    // AsyncTask để tải và render PDF
    private class DownloadAndRenderTask extends AsyncTask<String, Void, List<Bitmap>> {
        private final int maxPages;
        private File pdfFile;

        public DownloadAndRenderTask(int maxPages) {
            this.maxPages = maxPages;
        }

        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            String pdfUrl = urls[0];
            List<Bitmap> bitmaps = new ArrayList<>();
            pdfFile = new File(context.getCacheDir(), "preview.pdf");

            try {
                // 1. Tải file PDF từ URL
                URL url = new URL(pdfUrl);
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(pdfFile);

                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

                // 2. Mở PdfRenderer
                parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                pdfRenderer = new PdfRenderer(parcelFileDescriptor);

                int pageCount = pdfRenderer.getPageCount();
                int pagesToRender = Math.min(pageCount, maxPages);

                // 3. Render các trang (Sử dụng tỷ lệ 2x để khắc phục lỗi màn hình trắng do kích thước quá nhỏ)
                for (int i = 0; i < pagesToRender; i++) {
                    PdfRenderer.Page page = pdfRenderer.openPage(i);

                    // Lấy kích thước gốc và tăng tỷ lệ render
                    int pageWidth = page.getWidth();
                    int pageHeight = page.getHeight();
                    float scale = 2f;
                    int renderWidth = (int) (pageWidth * scale);
                    int renderHeight = (int) (pageHeight * scale);

                    Bitmap bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
                    android.graphics.Rect rect = new android.graphics.Rect(0, 0, renderWidth, renderHeight);

                    page.render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    bitmaps.add(bitmap);
                    page.close();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading PDF for preview: " + e.getMessage(), e);
                return null;
            }
            return bitmaps;
        }

        @Override
        protected void onPostExecute(List<Bitmap> bitmaps) {
            if (bitmaps != null && !bitmaps.isEmpty()) {
                // 4. Thiết lập Adapter cho ViewPager
                PdfPagerAdapter adapter = new PdfPagerAdapter(bitmaps);
                viewPager.setAdapter(adapter);
            } else {
                Toast.makeText(context, "Không thể tải xem trước PDF. Kiểm tra Logcat.", Toast.LENGTH_SHORT).show();
            }
            // Xóa file tạm thời
            if (pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }
        }
    }

    // Adapter đơn giản cho ViewPager2 hiển thị Bitmaps
    private static class PdfPagerAdapter extends RecyclerView.Adapter<PdfPagerAdapter.PageViewHolder> {
        private final List<Bitmap> pages;

        public PdfPagerAdapter(List<Bitmap> pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            // Thiết lập LayoutParams để ImageView lấp đầy ViewPager
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new PageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            ((ImageView) holder.itemView).setImageBitmap(pages.get(position));
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        public static class PageViewHolder extends RecyclerView.ViewHolder {
            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}