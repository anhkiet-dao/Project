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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.data.AppDatabase;
import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity;

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
    private final AppDatabase db;

    public PdfViewerUtility(Context context, ViewPager2 viewPager) {
        this.context = context;
        this.viewPager = viewPager;
        this.db = AppDatabase.getDatabase(context); // Khởi tạo Room
    }

    public void loadPdfPreview(Book book, int maxPages) {
        new DownloadAndRenderTask(maxPages,
                book.getId(),
                book.getName(),
                book.getAuthor(),
                book.getImageUrl())
                .execute(book.getLink());
    }

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

    private class DownloadAndRenderTask extends AsyncTask<String, Void, List<Bitmap>> {
        private final int maxPages;
        private final String storyId;
        private final String storyName;
        private final String storyAuthor;
        private final String coverImageUrl;
        private File pdfFile;
        private final DownloadedPdfDao pdfDao;

        public DownloadAndRenderTask(int maxPages, String storyId, String storyName, String storyAuthor, String coverImageUrl) {
            this.maxPages = maxPages;
            this.storyId = storyId;
            this.storyName = storyName;
            this.storyAuthor = storyAuthor;
            this.coverImageUrl = coverImageUrl;
            this.pdfDao = db.downloadedPdfDao();
        }

        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            String pdfUrl = urls[0];

            DownloadedPdfEntity cachedPdf = pdfDao.getPdfByUrl(pdfUrl);

            if (cachedPdf != null && cachedPdf.localFilePath != null) {
                pdfFile = new File(cachedPdf.localFilePath);
                if (pdfFile.exists()) {
                    Log.d(TAG, "Cache found! Rendering from local file: " + cachedPdf.localFilePath);
                    return renderPdfFromFile(pdfFile);
                }
            }

            Log.d(TAG, "Cache not found. Downloading PDF.");
            pdfFile = new File(context.getFilesDir(), storyId + "_preview_" + pdfUrl.hashCode() + ".pdf");


            try {
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

                DownloadedPdfEntity newPdf = new DownloadedPdfEntity();
                newPdf.storyDocumentId = storyId;
                newPdf.fileName = pdfFile.getName();
                newPdf.localFilePath = pdfFile.getAbsolutePath();
                newPdf.pdfUrl = pdfUrl;
                newPdf.author = storyAuthor;
                newPdf.coverImageUrl = coverImageUrl;

                newPdf.isCache = true;

                pdfDao.insert(newPdf);
                Log.d(TAG, "PDF downloaded and cached in Room for next time.");

                return renderPdfFromFile(pdfFile);

            } catch (Exception e) {
                Log.e(TAG, "Error loading PDF for preview: " + e.getMessage(), e);
                // Nếu tải thất bại, xóa file tạm để không gây lỗi lần sau
                if (pdfFile != null && pdfFile.exists()) {
                    pdfFile.delete();
                }
                return null;
            }
        }

        private List<Bitmap> renderPdfFromFile(File file) {
            List<Bitmap> bitmaps = new ArrayList<>();
            try {
                // Đảm bảo đóng renderer và file descriptor cũ nếu có
                if (pdfRenderer != null) pdfRenderer.close();
                if (parcelFileDescriptor != null) parcelFileDescriptor.close();

                parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                pdfRenderer = new PdfRenderer(parcelFileDescriptor);

                int pageCount = pdfRenderer.getPageCount();
                int pagesToRender = Math.min(pageCount, maxPages);

                for (int i = 0; i < pagesToRender; i++) {
                    PdfRenderer.Page page = pdfRenderer.openPage(i);

                    int pageWidth = page.getWidth();
                    int pageHeight = page.getHeight();
                    // Gợi ý: Giảm scale để render nhanh hơn, ví dụ 1.5f thay vì 2f
                    float scale = 1.5f;
                    int renderWidth = (int) (pageWidth * scale);
                    int renderHeight = (int) (pageHeight * scale);

                    Bitmap bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
                    android.graphics.Rect rect = new android.graphics.Rect(0, 0, renderWidth, renderHeight);

                    page.render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    bitmaps.add(bitmap);
                    page.close();
                }
                return bitmaps;

            } catch (IOException e) {
                Log.e(TAG, "Error rendering PDF from file: " + file.getAbsolutePath(), e);
                return null;
            }
        }


        @Override
        protected void onPostExecute(List<Bitmap> bitmaps) {
            if (bitmaps != null && !bitmaps.isEmpty()) {
                PdfPagerAdapter adapter = new PdfPagerAdapter(bitmaps);
                viewPager.setAdapter(adapter);
            } else {
                Toast.makeText(context, "Không thể tải xem trước PDF. Kiểm tra Logcat.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static class PdfPagerAdapter extends RecyclerView.Adapter<PdfPagerAdapter.PageViewHolder> {
        private final List<Bitmap> pages;

        public PdfPagerAdapter(List<Bitmap> pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
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