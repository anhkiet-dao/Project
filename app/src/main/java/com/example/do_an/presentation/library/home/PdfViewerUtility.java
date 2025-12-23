package com.example.do_an.presentation.library.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.core.database.AppDatabase;
import com.example.do_an.data.reading.local.dao.CachePdfDao;
import com.example.do_an.data.reading.local.entity.CachePdfEntity;
import com.example.do_an.domain.library.model.Book;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
    private final CachePdfDao cachePdfDao;
    private DownloadAndRenderTask currentTask;

    public PdfViewerUtility(Context context, ViewPager2 viewPager) {
        this.context = context;
        this.viewPager = viewPager;
        this.db = AppDatabase.getDatabase(context);
        this.cachePdfDao = db.cachePdfDao();
    }

    public void loadPdfPreview(Book book, int maxPages) {
        if (currentTask != null) currentTask.cancel(true);
        closeRenderer();

        currentTask = new DownloadAndRenderTask(maxPages,
                book.getId(),
                book.getName(),
                book.getAuthor(),
                book.getImageUrl());
        currentTask.execute(book.getLink());
    }

    public void preloadPdf(Book book) {
        if (book == null || book.getLink() == null || book.getLink().isEmpty()) return;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    CachePdfEntity cachedPdf = cachePdfDao.getCacheByStoryId(book.getId());
                    if (cachedPdf != null && cachedPdf.localFilePath != null
                            && new File(cachedPdf.localFilePath).exists()) return null;

                    File pdfFile = new File(context.getCacheDir(),
                            book.getId() + "_preview_" + book.getLink().hashCode() + ".pdf");

                    URL url = new URL(book.getLink());
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    InputStream input = new BufferedInputStream(url.openStream());
                    OutputStream output = new FileOutputStream(pdfFile);

                    byte[] data = new byte[1024];
                    int count;
                    while ((count = input.read(data)) != -1) output.write(data, 0, count);

                    output.flush();
                    output.close();
                    input.close();

                    CachePdfEntity newPdf = new CachePdfEntity();
                    newPdf.storyDocumentId = book.getId();
                    newPdf.fileName = pdfFile.getName();
                    newPdf.localFilePath = pdfFile.getAbsolutePath();
                    newPdf.pdfUrl = book.getLink();
                    cachePdfDao.insert(newPdf);

                    Log.d(TAG, "PDF preloaded and cached: " + book.getName());

                } catch (Exception e) {
                    Log.e(TAG, "Error preloading PDF: " + book.getName(), e);
                }
                return null;
            }
        }.execute();
    }

    /** Đóng renderer cũ */
    public void closeRenderer() {
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
                parcelFileDescriptor = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing PDF renderer", e);
        }
    }

    private class DownloadAndRenderTask extends AsyncTask<String, Void, List<Bitmap>> {
        private final int maxPages;
        private final String storyId;
        private final String storyName;
        private final String storyAuthor;
        private final String coverImageUrl;
        private File pdfFile;

        public DownloadAndRenderTask(int maxPages, String storyId, String storyName, String storyAuthor, String coverImageUrl) {
            this.maxPages = maxPages;
            this.storyId = storyId;
            this.storyName = storyName;
            this.storyAuthor = storyAuthor;
            this.coverImageUrl = coverImageUrl;
        }

        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            if (isCancelled()) return null;

            String pdfUrl = urls[0];
            CachePdfEntity cachedPdf = cachePdfDao.getCacheByStoryId(storyId);

            if (cachedPdf != null && cachedPdf.localFilePath != null) {
                pdfFile = new File(cachedPdf.localFilePath);
                if (pdfFile.exists()) {
                    Log.d(TAG, "Cache found! Rendering from local file: " + cachedPdf.localFilePath);
                    return renderPdfFromFile(pdfFile);
                } else {
                    cachePdfDao.delete(cachedPdf);
                    Log.d(TAG, "Cache existed but file missing. Deleted entity.");
                }
            }

            Log.d(TAG, "Downloading PDF: " + storyName);
            pdfFile = new File(context.getCacheDir(), storyId + "_preview_" + pdfUrl.hashCode() + ".pdf");

            try {
                URL url = new URL(pdfUrl);
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(pdfFile);

                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        output.close();
                        return null;
                    }
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

                CachePdfEntity newPdf = new CachePdfEntity();
                newPdf.storyDocumentId = storyId;
                newPdf.fileName = pdfFile.getName();
                newPdf.localFilePath = pdfFile.getAbsolutePath();
                newPdf.pdfUrl = pdfUrl;
                cachePdfDao.insert(newPdf);

                Log.d(TAG, "PDF downloaded and cached: " + storyName);

                return renderPdfFromFile(pdfFile);

            } catch (Exception e) {
                Log.e(TAG, "Error loading PDF preview: " + storyName, e);
                if (pdfFile != null && pdfFile.exists()) pdfFile.delete();
                return null;
            }
        }

        private List<Bitmap> renderPdfFromFile(File file) {
            List<Bitmap> bitmaps = new ArrayList<>();
            try {
                if (isCancelled()) return null;

                if (pdfRenderer != null) pdfRenderer.close();
                if (parcelFileDescriptor != null) parcelFileDescriptor.close();

                parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                pdfRenderer = new PdfRenderer(parcelFileDescriptor);

                int pageCount = pdfRenderer.getPageCount();
                int pagesToRender = Math.min(pageCount, maxPages);

                for (int i = 0; i < pagesToRender; i++) {
                    if (isCancelled()) return null;

                    PdfRenderer.Page page = pdfRenderer.openPage(i);
                    int pageWidth = page.getWidth();
                    int pageHeight = page.getHeight();
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

            } catch (Exception e) {
                Log.e(TAG, "Error rendering PDF from file: " + file.getAbsolutePath(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Bitmap> bitmaps) {
            if (isCancelled()) return;

            if (bitmaps != null && !bitmaps.isEmpty()) {
                PdfPagerAdapter adapter = new PdfPagerAdapter(bitmaps);
                viewPager.setAdapter(adapter);
                viewPager.setCurrentItem(0, false);
            } else {
                Toast.makeText(context, "Không thể tải xem trước PDF.", Toast.LENGTH_SHORT).show();
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
            public PageViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
            }
        }
    }
}
