package com.example.do_an.application;

import android.app.Activity; // Dùng Activity chung
import android.content.Context;
import android.os.Handler; // Dùng Handler để chạy trên UI thread nếu Context không phải Activity
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
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
    // Khởi tạo Handler để đảm bảo các tiến trình Toast chạy trên UI thread
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private TextView txtPageIndicator;

    public interface PdfSetupCallback { void setup(File pdfFile); }
    public interface StringConsumer { void set(String value); }
    // Khuyến nghị thêm callback cho việc ẩn ProgressBar tải xuống vĩnh viễn
    public interface LoadingListener {
        void showLoading();
        void hideLoading();
        void hideDownloadProgress(); // <<< THÊM: Callback ẩn ProgressBar (dùng cho downloadPdfWithOkHttp)
    }
    public void setTxtPageIndicator(TextView textView) { this.txtPageIndicator = textView; }
    private LoadingListener loadingListener;

    public DownloadManager(Context context) {
        this.context = context;
    }

    public void setIsActivityDestroyed(boolean isDestroyed) { this.isActivityDestroyed = isDestroyed; }

    public void setLoadingListener(LoadingListener listener) { this.loadingListener = listener; }

    // SỬA: Thay thế việc ép kiểu ReadActivity bằng cách dùng Handler hoặc Activity chung
    private void runOnUiThread(Runnable action) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(action);
        } else {
            // Trường hợp Context không phải Activity (rất hiếm trong trường hợp này)
            uiHandler.post(action);
        }
    }

    private void hideLoadingOnUi() {
        if (loadingListener != null) runOnUiThread(() -> loadingListener.hideLoading());
    }

    // download PDF lưu vĩnh viễn (SỬA: Loại bỏ truy cập View trực tiếp)
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
                    // SỬA: Thay thế truy cập View bằng callback
                    if (loadingListener != null) loadingListener.hideDownloadProgress();
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
                        // SỬA: Thay thế truy cập View bằng callback
                        if (loadingListener != null) loadingListener.hideDownloadProgress();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(context, "Lỗi tải PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // SỬA: Thay thế truy cập View bằng callback
                        if (loadingListener != null) loadingListener.hideDownloadProgress();
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
                hideLoadingOnUi(); // Luôn gọi ẩn loading sau khi thất bại
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream is = null;
                FileOutputStream fos = null;
                File pdfFile = null;

                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> Toast.makeText(context, "Không tải thành công dữ liệu!", Toast.LENGTH_SHORT).show());
                        hideLoadingOnUi(); // Ẩn loading nếu phản hồi không thành công
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
                            // Không gọi hideLoadingOnUi() vì nó đã bị hủy một cách chủ động
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

                } catch (Exception e) { handleDownloadError(e, call); hideLoadingOnUi(); } // Ẩn loading khi có Exception
                finally {
                    try { if (is != null) is.close(); if (fos != null) fos.close(); if (response != null) response.close(); }
                    catch (IOException ignored) {}
                    if (call.isCanceled() && pdfFile != null) pdfFile.delete();
                    // hideLoadingOnUi() đã được gọi trong catch/failure/success
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
        // Cần gọi hideLoadingOnUi() ở đây nếu Firestore thất bại hoặc không có link PDF
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
                                hideLoadingOnUi(); // Ẩn loading nếu không có link PDF
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Không tìm thấy dữ liệu PDF cho truyện '" + storyDocumentId + "'!", Toast.LENGTH_SHORT).show();
                            hideLoadingOnUi(); // Ẩn loading nếu không tìm thấy document
                        });
                    }
                })
                .addOnFailureListener(e ->
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Lỗi tải Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            hideLoadingOnUi(); // Ẩn loading khi lỗi Firestore
                        }));
    }

    public Call loadAndSetupPdf(String episodePdfLink, String pdfPath, String mainStoryTitle,
                                PdfSetupCallback callback, StringConsumer urlConsumer) {
        if (loadingListener != null && episodePdfLink == null && pdfPath == null) loadingListener.showLoading(); // Đảm bảo showLoading nếu cần gọi Firestore

        if (episodePdfLink != null && !episodePdfLink.isEmpty()) {
            urlConsumer.set(episodePdfLink);
            return downloadPdfToCache(episodePdfLink, "temp_episode.pdf", callback);
        } else if (pdfPath != null) {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                callback.setup(pdfFile);
                if (loadingListener != null) runOnUiThread(() -> loadingListener.hideLoading()); // Ẩn loading khi đọc file local thành công
                findAndSetCurrentReadUrl(mainStoryTitle, urlConsumer);
            } else {
                Toast.makeText(context, "File PDF không tồn tại, tải lại...", Toast.LENGTH_SHORT).show();
                // Load từ Firestore sẽ gọi showLoading() trong loadPdfFromFirestore
                loadPdfFromFirestore(mainStoryTitle, callback, urlConsumer);
            }
        } else {
            // Load từ Firestore sẽ gọi showLoading() trong loadPdfFromFirestore
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