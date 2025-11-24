package com.example.do_an.application;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.do_an.R;
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

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private boolean isActivityDestroyed = false;
    private final int OPTIMIZED_BUFFER_SIZE = 256 * 1024; // 256 KB

    public interface PdfSetupCallback { void setup(File pdfFile); }
    public interface StringConsumer { void set(String value); }
    public interface LoadingListener { void showLoading(); void hideLoading(); }

    private LoadingListener loadingListener;

    public DownloadManager(Context context) {
        this.context = context;
    }

    public void setIsActivityDestroyed(boolean isDestroyed) { this.isActivityDestroyed = isDestroyed; }

    public void setLoadingListener(LoadingListener listener) { this.loadingListener = listener; }

    private void runOnUiThread(Runnable action) {
        if (context instanceof ReadActivity) ((ReadActivity) context).runOnUiThread(action);
    }

    private void hideLoadingOnUi() {
        if (loadingListener != null) runOnUiThread(() -> loadingListener.hideLoading());
    }

    // --- Chỉ dùng URL trực tiếp, bỏ convertDriveUrl ---
// download PDF lưu vĩnh viễn
    public void downloadPdfWithOkHttp(String pdfUrl, String fileName) {
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
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(context, "Lỗi tải PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    ((ReadActivity)context).findViewById(R.id.progressDownload).setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> Toast.makeText(context, "Không tải được PDF!", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    is = response.body().byteStream();
                    File pdfDir = new File(context.getExternalFilesDir(null), "PDF");
                    if (!pdfDir.exists()) pdfDir.mkdirs();
                    File pdfFile = new File(pdfDir, fileName);

                    fos = new FileOutputStream(pdfFile);
                    byte[] buffer = new byte[OPTIMIZED_BUFFER_SIZE];
                    int len;
                    while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                    fos.flush();

                    runOnUiThread(() -> {
                        Toast.makeText(context, "Tải xuống thành công!", Toast.LENGTH_SHORT).show();
                        ((ReadActivity)context).findViewById(R.id.progressDownload).setVisibility(View.GONE);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(context, "Lỗi tải PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        ((ReadActivity)context).findViewById(R.id.progressDownload).setVisibility(View.GONE);
                    });
                } finally {
                    try { if (fos != null) fos.close(); if (is != null) is.close(); if (response != null) response.close(); }
                    catch (IOException ignored) {}
                }
            }
        });
    }

    // --- Tải PDF vào cache
    private Call downloadPdfToCache(String pdfUrl, String fileName, PdfSetupCallback callback) {
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
                        return;
                    }

                    is = response.body().byteStream();
                    File cacheDir = new File(context.getCacheDir(), "PDF");
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    pdfFile = new File(cacheDir, fileName);

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
                    runOnUiThread(() -> callback.setup(finalPdfFile));

                } catch (Exception e) { handleDownloadError(e, call); }
                finally {
                    try { if (is != null) is.close(); if (fos != null) fos.close(); if (response != null) response.close(); }
                    catch (IOException ignored) {}
                    if (call.isCanceled() && pdfFile != null) pdfFile.delete();
                    hideLoadingOnUi();
                }
            }
        });

        return downloadCall;
    }

    private void handleDownloadError(Exception e, Call call) {
        if (!isActivityDestroyed && !call.isCanceled()) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(context, "Lỗi tải PDF!", Toast.LENGTH_SHORT).show());
        }
    }

    // === Các hàm Firestore giữ nguyên nếu cần
    private void loadPdfFromFirestore(String storyDocumentId, PdfSetupCallback callback, StringConsumer urlConsumer) {
        db.collection("Truyentranh").document(storyDocumentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String pdfUrl = doc.getString("pdfUrl");
                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            urlConsumer.set(pdfUrl);
                            downloadPdfToCache(pdfUrl, "temp_story.pdf", callback);
                        } else {
                            Toast.makeText(context, "Truyện này không có file PDF!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Không tìm thấy dữ liệu PDF cho truyện '" + storyDocumentId + "'!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Lỗi tải Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    public Call loadAndSetupPdf(String episodePdfLink, String pdfPath, String mainStoryTitle,
                                PdfSetupCallback callback, StringConsumer urlConsumer) {
        if (episodePdfLink != null && !episodePdfLink.isEmpty()) {
            urlConsumer.set(episodePdfLink);
            return downloadPdfToCache(episodePdfLink, "temp_episode.pdf", callback);
        } else if (pdfPath != null) {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                callback.setup(pdfFile);
                if (loadingListener != null) runOnUiThread(() -> loadingListener.hideLoading());
                findAndSetCurrentReadUrl(mainStoryTitle, urlConsumer);
            } else {
                Toast.makeText(context, "File PDF không tồn tại, tải lại...", Toast.LENGTH_SHORT).show();
                loadPdfFromFirestore(mainStoryTitle, callback, urlConsumer);
            }
        } else {
            loadPdfFromFirestore(mainStoryTitle, callback, urlConsumer);
        }
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
