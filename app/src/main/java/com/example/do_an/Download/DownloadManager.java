package com.example.do_an.Download;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity;

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final DownloadedPdfDao pdfDao;
    private boolean isActivityDestroyed = false;
    private final int OPTIMIZED_BUFFER_SIZE = 256 * 1024;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private TextView txtPageIndicator;

    public interface PdfSetupCallback { void setup(File pdfFile); }
    public interface StringConsumer { void set(String value); }
    public interface LoadingListener {
        void showLoading();
        void hideLoading();
        void hideDownloadProgress();
    }
    public void setTxtPageIndicator(TextView textView) { this.txtPageIndicator = textView; }
    private LoadingListener loadingListener;

    public DownloadManager(Context context, DownloadedPdfDao pdfDao) {
        this.context = context;
        this.pdfDao = pdfDao;
    }

    public void setIsActivityDestroyed(boolean isDestroyed) { this.isActivityDestroyed = isDestroyed; }

    public void setLoadingListener(LoadingListener listener) { this.loadingListener = listener; }

    private void runOnUiThread(Runnable action) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(action);
        } else {
            uiHandler.post(action);
        }
    }

    private void hideLoadingOnUi() {
        if (loadingListener != null) runOnUiThread(() -> loadingListener.hideLoading());
    }

    private void handleDownloadError(Exception e, Call call) {
        if (!isActivityDestroyed && !call.isCanceled()) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(context, "Lỗi tải PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Tải PDF và lưu thông tin vào Room, bao gồm URL ảnh bìa.
     * @param pdfUrl URL của file PDF.
     * @param fileName Tên file để lưu.
     * @param storyDocumentId ID của truyện.
     * @param author Tác giả.
     * @param coverImageUrl URL ảnh bìa. ⬅️ ĐÃ THÊM THAM SỐ THỨ 5
     */
    public void downloadPdfWithOkHttp(final String pdfUrl, final String fileName, final String storyDocumentId, final String author, final String coverImageUrl) {

        new Thread(() -> {
            DownloadedPdfEntity existingPdf = pdfDao.getPdfByUrl(pdfUrl);

            if (existingPdf != null) {
                runOnUiThread(() -> {
                    Toast.makeText(context, "File đã được tải xuống trước đó.", Toast.LENGTH_SHORT).show();
                    if (loadingListener != null) loadingListener.hideDownloadProgress();
                });
                return;
            }

            runOnUiThread(() -> Toast.makeText(context, "Bắt đầu tải xuống ...", Toast.LENGTH_SHORT).show());

            final String downloadUrl = pdfUrl;

            OkHttpClient client = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder().url(downloadUrl).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    handleDownloadError(e, call);
                    runOnUiThread(() -> {
                        if (loadingListener != null) loadingListener.hideDownloadProgress();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    InputStream is = null;
                    FileOutputStream fos = null;
                    File pdfFile = null;

                    try {
                        if (!response.isSuccessful() || response.body() == null) {
                            runOnUiThread(() -> Toast.makeText(context, "Không tải được PDF!", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        is = response.body().byteStream();
                        File pdfDir = new File(context.getExternalFilesDir(null), "PDF");
                        if (!pdfDir.exists()) pdfDir.mkdirs();
                        pdfFile = new File(pdfDir, fileName);

                        fos = new FileOutputStream(pdfFile);
                        byte[] buffer = new byte[OPTIMIZED_BUFFER_SIZE];
                        int len;
                        while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                        fos.flush();

                        final File finalPdfFile = pdfFile;

                        // LƯU ENTITY VÀO ROOM
                        DownloadedPdfEntity entity = new DownloadedPdfEntity();
                        entity.storyDocumentId = storyDocumentId;
                        entity.fileName = fileName;
                        entity.pdfUrl = pdfUrl;
                        entity.localFilePath = finalPdfFile.getAbsolutePath();
                        entity.author = author;
                        entity.coverImageUrl = coverImageUrl; // ⬅️ LƯU THÔNG TIN ẢNH BÌA

                        pdfDao.insert(entity);

                        runOnUiThread(() -> {
                            Toast.makeText(context, "Tải xuống thành công và lưu CSDL!", Toast.LENGTH_SHORT).show();
                            if (loadingListener != null) loadingListener.hideDownloadProgress();
                        });

                    } catch (Exception e) {
                        handleDownloadError(e, call);
                        runOnUiThread(() -> {
                            if (loadingListener != null) loadingListener.hideDownloadProgress();
                        });
                    } finally {
                        try { if (fos != null) fos.close(); if (is != null) is.close(); if (response != null) response.close(); }
                        catch (IOException ignored) {}
                    }
                }
            });
        }).start();
    }

    private Call downloadPdfToCache(final String pdfUrl, final String fileName, final PdfSetupCallback callback) {
        if (loadingListener != null) loadingListener.showLoading();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        final String downloadUrl = pdfUrl;
        Request request = new Request.Builder().url(downloadUrl).build();
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
                        runOnUiThread(() -> Toast.makeText(context, "Không tải thành công dữ liệu!", Toast.LENGTH_SHORT).show());
                        hideLoadingOnUi();
                        return;
                    }

                    is = response.body().byteStream();
                    File cacheDir = new File(context.getCacheDir(), "PDF");
                    if (!cacheDir.exists()) cacheDir.mkdirs();

                    final File finalPdfFileReference = new File(cacheDir, fileName);
                    pdfFile = finalPdfFileReference;

                    fos = new FileOutputStream(pdfFile);
                    byte[] buffer = new byte[OPTIMIZED_BUFFER_SIZE];
                    int len;

                    while ((len = is.read(buffer)) != -1) {
                        if (isActivityDestroyed || call.isCanceled()) {
                            Log.d(TAG, "Đã hủy tải do thoát màn hình hoặc bị hủy thủ công");
                            return;
                        }
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();

                    final File finalPdfFile = pdfFile;

                    runOnUiThread(() -> {
                        callback.setup(finalPdfFile);
                        if (txtPageIndicator != null) {
                            txtPageIndicator.setVisibility(View.VISIBLE);
                        }
                        hideLoadingOnUi();
                    });

                } catch (Exception e) { handleDownloadError(e, call); hideLoadingOnUi(); }
                finally {
                    try { if (is != null) is.close(); if (fos != null) fos.close(); if (response != null) response.close(); }
                    catch (IOException ignored) {}
                    if (call.isCanceled() && pdfFile != null) pdfFile.delete();
                }
            }
        });

        return downloadCall;
    }

    private void loadPdfFromFirestore(final String storyDocumentId, final PdfSetupCallback callback, final StringConsumer urlConsumer) {
        db.collection("Truyentranh").document(storyDocumentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String pdfUrl = doc.getString("pdfUrl");
                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            urlConsumer.set(pdfUrl);
                            downloadPdfToCache(pdfUrl, "temp_story.pdf", callback);
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(context, "Truyện này không có file PDF!", Toast.LENGTH_SHORT).show();
                                hideLoadingOnUi();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Không tìm thấy dữ liệu PDF cho truyện '" + storyDocumentId + "'!", Toast.LENGTH_SHORT).show();
                            hideLoadingOnUi();
                        });
                    }
                })
                .addOnFailureListener(e ->
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Lỗi tải Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            hideLoadingOnUi();
                        }));
    }

    public Call loadAndSetupPdf(final String episodePdfLink, final String pdfPath, final String mainStoryTitle,
                                final PdfSetupCallback callback, final StringConsumer urlConsumer) {

        if (loadingListener != null) loadingListener.showLoading();

        if (pdfPath != null) {
            final File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                Log.d(TAG, "Load PDF: Đọc từ đường dẫn cục bộ (pdfPath).");
                callback.setup(pdfFile);
                if (loadingListener != null) runOnUiThread(() -> loadingListener.hideLoading());
                findAndSetCurrentReadUrl(mainStoryTitle, urlConsumer);
                return null;
            } else {
                Log.e(TAG, "Load PDF: File đã tải xuống bị mất tại đường dẫn: " + pdfPath);

                runOnUiThread(() -> {
                    Toast.makeText(context, "Lỗi: Không tìm thấy file đã tải xuống.", Toast.LENGTH_LONG).show();
                    if (loadingListener != null) loadingListener.hideLoading();
                });
                return null;
            }
        }

        if (episodePdfLink != null && !episodePdfLink.isEmpty()) {
            Log.d(TAG, "Load PDF: Tải file từ Link Tập (episodePdfLink).");
            urlConsumer.set(episodePdfLink);
            return downloadPdfToCache(episodePdfLink, "temp_episode.pdf", callback);
        }

        new Thread(() -> {
            final DownloadedPdfEntity localPdf = pdfDao.getPdfByStoryId(mainStoryTitle);

            runOnUiThread(() -> {
                if (localPdf != null) {
                    final File pdfFile = new File(localPdf.localFilePath);

                    if (pdfFile.exists()) {
                        Log.d(TAG, "Load PDF: Đọc từ Room (File tồn tại).");
                        callback.setup(pdfFile);
                        if (loadingListener != null) loadingListener.hideLoading();
                        urlConsumer.set(localPdf.pdfUrl);
                        if (txtPageIndicator != null) txtPageIndicator.setVisibility(View.VISIBLE);
                        return;
                    } else {
                        Log.d(TAG, "Load PDF: File Room bị mất, đang xóa record và tải lại.");
                        Toast.makeText(context, "File tải xuống bị mất, đang tải lại...", Toast.LENGTH_SHORT).show();
                        new Thread(() -> pdfDao.delete(localPdf)).start();
                        loadPdfFromFirestore(mainStoryTitle, callback, urlConsumer);
                    }
                } else {
                    Log.d(TAG, "Load PDF: Không tìm thấy trong Room hay Link Tập. Tải từ Firestore (link chính).");
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
                        if (pdfUrl != null && !pdfUrl.isEmpty()) urlConsumer.set(pdfUrl);
                    }
                });
    }

}