package com.example.do_an.presentation.library.downloads;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.do_an.R;
import com.example.do_an.data.library.local.dao.DownloadedPdfDao;
import com.example.do_an.data.library.local.entity.DownloadedPdfEntity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadController {
    private static final String TAG = "DownloadManager";
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final DownloadedPdfDao pdfDao;
    private boolean isActivityDestroyed = false;
    private final int OPTIMIZED_BUFFER_SIZE = 1024 * 1024;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private View progressContainer;

    public interface PdfSetupCallback {
        void setup(File pdfFile);
    }

    public interface StringConsumer {
        void set(String value);
    }

    public interface LoadingListener {
        void showLoading();

        void hideLoading();

        void hideDownloadProgress();
    }

    public void setProgressContainer(View progressContainer) {
        this.progressContainer = progressContainer;
    }

    private LoadingListener loadingListener;

    public DownloadController(Context context, DownloadedPdfDao pdfDao) {
        this.context = context;
        this.pdfDao = pdfDao;
    }

    public void setIsActivityDestroyed(boolean isDestroyed) {
        this.isActivityDestroyed = isDestroyed;
    }

    public void setLoadingListener(LoadingListener listener) {
        this.loadingListener = listener;
    }

    private void runOnUiThread(Runnable action) {
        if (context instanceof Activity)
            ((Activity) context).runOnUiThread(action);
        else
            uiHandler.post(action);
    }

    private void hideLoadingOnUi() {
        if (loadingListener != null)
            runOnUiThread(() -> loadingListener.hideLoading());
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private void logDebug(String message) {
        Log.d(TAG, message);
    }

    private void logError(String message) {
        Log.e(TAG, message);
    }

    private void handleDownloadError(Exception e, Call call) {
        if (!isActivityDestroyed && !call.isCanceled()) {
            e.printStackTrace();
            showToast(context.getString(R.string.pdf_download_error, e.getMessage()));
            logError(context.getString(R.string.pdf_download_error, e.getMessage()));
        }
    }

    public void downloadPdfWithOkHttp(final String pdfUrl, final String fileName, final String storyDocumentId,
            final String author, final String coverImageUrl) {

        new Thread(() -> {
            DownloadedPdfEntity existingPdf = pdfDao.getPdfByFileName(fileName);
            File pdfDir = new File(context.getExternalFilesDir(null), "PDF");
            if (!pdfDir.exists())
                pdfDir.mkdirs();
            File localFile = new File(pdfDir, fileName);

            if (localFile.exists()) {
                @SuppressLint({ "StringFormatInvalid", "LocalSuppress" })
                String msg = context.getString(R.string.file_already_downloaded, fileName.replace(".pdf", ""));
                showToast(msg);
                logDebug(msg);
                if (loadingListener != null)
                    runOnUiThread(() -> loadingListener.hideDownloadProgress());
                return;
            } else if (existingPdf != null) {
                pdfDao.delete(existingPdf);
                logDebug(context.getString(R.string.entity_deleted_for_redownload));
            }

            showToast(context.getString(R.string.start_download));
            logDebug(context.getString(R.string.start_download));

            OkHttpClient client = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder().url(pdfUrl).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    handleDownloadError(e, call);
                    if (loadingListener != null)
                        runOnUiThread(() -> loadingListener.hideDownloadProgress());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    InputStream is = null;
                    FileOutputStream fos = null;

                    try {
                        if (!response.isSuccessful() || response.body() == null) {
                            showToast(context.getString(R.string.pdf_not_downloaded));
                            if (loadingListener != null)
                                runOnUiThread(() -> loadingListener.hideDownloadProgress());
                            logError(context.getString(R.string.pdf_not_downloaded));
                            return;
                        }

                        is = response.body().byteStream();
                        fos = new FileOutputStream(localFile);
                        byte[] buffer = new byte[OPTIMIZED_BUFFER_SIZE];
                        int len;
                        while ((len = is.read(buffer)) != -1)
                            fos.write(buffer, 0, len);
                        fos.flush();

                        final File finalPdfFile = localFile;

                        DownloadedPdfEntity entity = new DownloadedPdfEntity();
                        entity.storyDocumentId = storyDocumentId;
                        entity.fileName = fileName;
                        entity.pdfUrl = pdfUrl;
                        entity.localFilePath = finalPdfFile.getAbsolutePath();
                        entity.author = author;
                        entity.coverImageUrl = coverImageUrl;
                        pdfDao.insert(entity);

                        showToast(context.getString(R.string.download_success));
                        logDebug(context.getString(R.string.download_success));
                        if (loadingListener != null)
                            runOnUiThread(() -> loadingListener.hideDownloadProgress());

                    } catch (Exception e) {
                        handleDownloadError(e, call);
                        if (loadingListener != null)
                            runOnUiThread(() -> loadingListener.hideDownloadProgress());
                    } finally {
                        try {
                            if (fos != null)
                                fos.close();
                            if (is != null)
                                is.close();
                            if (response != null)
                                response.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            });
        }).start();
    }

    private Call downloadPdfToCache(final String pdfUrl, final String fileName, final PdfSetupCallback callback) {
        if (loadingListener != null)
            runOnUiThread(() -> loadingListener.showLoading());

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(pdfUrl).build();
        final Call downloadCall = client.newCall(request);

        downloadCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleDownloadError(e, call);
                hideLoadingOnUi();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream is = null;
                FileOutputStream fos = null;
                File pdfFile = null;

                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        showToast(context.getString(R.string.pdf_not_downloaded));
                        hideLoadingOnUi();
                        logError(context.getString(R.string.pdf_not_downloaded));
                        return;
                    }

                    is = response.body().byteStream();
                    File cacheDir = new File(context.getCacheDir(), "PDF");
                    if (!cacheDir.exists())
                        cacheDir.mkdirs();

                    pdfFile = new File(cacheDir, fileName);
                    fos = new FileOutputStream(pdfFile);
                    byte[] buffer = new byte[OPTIMIZED_BUFFER_SIZE];
                    int len;

                    while ((len = is.read(buffer)) != -1) {
                        if (isActivityDestroyed || call.isCanceled()) {
                            logDebug(context.getString(R.string.download_canceled));
                            return;
                        }
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();

                    final File finalPdfFile = pdfFile;

                    runOnUiThread(() -> {
                        callback.setup(finalPdfFile);
                        if (progressContainer != null)
                            progressContainer.setVisibility(View.VISIBLE);
                        hideLoadingOnUi();
                        logDebug(context.getString(R.string.download_ready));
                    });

                } catch (Exception e) {
                    handleDownloadError(e, call);
                    hideLoadingOnUi();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                        if (fos != null)
                            fos.close();
                        if (response != null)
                            response.close();
                    } catch (IOException ignored) {
                    }
                    if (call.isCanceled() && pdfFile != null)
                        pdfFile.delete();
                }
            }
        });

        return downloadCall;
    }

    @SuppressLint("StringFormatInvalid")
    private void loadPdfFromFirestore(final String storyDocumentId, final PdfSetupCallback callback,
            final StringConsumer urlConsumer) {
        db.collection("Truyentranh").document(storyDocumentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String pdfUrl = doc.getString("pdfUrl");
                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            urlConsumer.set(pdfUrl);
                            downloadPdfToCache(pdfUrl, "temp_story.pdf", callback);
                        } else {
                            showToast(context.getString(R.string.no_pdf_file));
                            hideLoadingOnUi();
                        }
                    } else {
                        showToast(context.getString(R.string.pdf_data_not_found, storyDocumentId));
                        hideLoadingOnUi();
                    }
                })
                .addOnFailureListener(e -> {
                    showToast(context.getString(R.string.firestore_load_error, e.getMessage()));
                    hideLoadingOnUi();
                });
    }

    public Call loadAndSetupPdf(final String episodePdfLink, final String pdfPath, final String mainStoryTitle,
            final PdfSetupCallback callback, final StringConsumer urlConsumer) {

        if (loadingListener != null)
            runOnUiThread(() -> loadingListener.showLoading());

        if (pdfPath != null) {
            final File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                logDebug(context.getString(R.string.load_pdf_local));
                callback.setup(pdfFile);
                if (loadingListener != null)
                    runOnUiThread(() -> loadingListener.hideLoading());
                findAndSetCurrentReadUrl(mainStoryTitle, urlConsumer);
                return null;
            } else {
                logError(context.getString(R.string.load_pdf_missing, pdfPath));
                showToast(context.getString(R.string.load_pdf_missing_toast));
                if (loadingListener != null)
                    runOnUiThread(() -> loadingListener.hideLoading());
                return null;
            }
        }

        if (episodePdfLink != null && !episodePdfLink.isEmpty()) {
            logDebug(context.getString(R.string.load_pdf_episode));
            urlConsumer.set(episodePdfLink);
            return downloadPdfToCache(episodePdfLink, "temp_episode.pdf", callback);
        }

        new Thread(() -> {
            final DownloadedPdfEntity localPdf = pdfDao.getPdfByStoryId(mainStoryTitle);

            runOnUiThread(() -> {
                if (localPdf != null) {
                    final File pdfFile = new File(localPdf.localFilePath);

                    if (pdfFile.exists()) {
                        logDebug(context.getString(R.string.load_pdf_room_exists));
                        callback.setup(pdfFile);
                        if (loadingListener != null)
                            loadingListener.hideLoading();
                        urlConsumer.set(localPdf.pdfUrl);
                        if (progressContainer != null)
                            progressContainer.setVisibility(View.VISIBLE);
                        return;
                    } else {
                        logDebug(context.getString(R.string.load_pdf_room_missing));
                        showToast(context.getString(R.string.file_lost_redownload));
                        new Thread(() -> pdfDao.delete(localPdf)).start();
                        loadPdfFromFirestore(mainStoryTitle, callback, urlConsumer);
                    }
                } else {
                    logDebug(context.getString(R.string.load_pdf_room_missing));
                    loadPdfFromFirestore(mainStoryTitle, callback, urlConsumer);
                }
            });
        }).start();

        return null;
    }

    private void findAndSetCurrentReadUrl(String storyDocumentId, StringConsumer urlConsumer) {
        db.collection("Truyentranh").document(storyDocumentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String pdfUrl = doc.getString("pdfUrl");
                        if (pdfUrl != null && !pdfUrl.isEmpty())
                            urlConsumer.set(pdfUrl);
                    }
                });
    }

}
