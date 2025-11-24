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

    // Khai báo Buffer Size tối ưu hóa
    private final int OPTIMIZED_BUFFER_SIZE = 256 * 1024; // 256 KB

    public interface PdfSetupCallback { void setup(File pdfFile); }
    public interface StringConsumer { void set(String value); }
    public interface LoadingListener {
        void showLoading();
        void hideLoading();
    }

    private LoadingListener loadingListener;

    public DownloadManager(Context context) {
        this.context = context;
    }

    public void setIsActivityDestroyed(boolean isDestroyed) {
        this.isActivityDestroyed = isDestroyed;
    }

    public void setLoadingListener(LoadingListener listener) {
        this.loadingListener = listener;
    }

    // Hàm hỗ trợ để thực thi trên UI thread
    private void runOnUiThread(Runnable action) {
        if (context instanceof ReadActivity) {
            ((ReadActivity) context).runOnUiThread(action);
        }
    }

    // Hàm hỗ trợ ẩn loading
    private void hideLoadingOnUi() {
        if (loadingListener != null) {
            runOnUiThread(() -> loadingListener.hideLoading());
        }
    }

    private String convertDriveUrl(String pdfUrl) {
        if (pdfUrl.contains("drive.google.com/file/d/")) {
            String[] parts = pdfUrl.split("/d/");
            if (parts.length > 1) {
                String fileId = parts[1].split("/")[0];
                return "https://drive.google.com/uc?export=download&id=" + fileId;
            }
        }
        return pdfUrl;
    }


    // --- Logic: Tải file PDF dùng OkHttp để lưu trữ VĨNH VIỄN (Sử dụng OkHttp Callback) ---
    public void downloadPdfWithOkHttp(String driveUrl, String fileName) {
        runOnUiThread(() -> Toast.makeText(context, "Bắt đầu tải xuống ...", Toast.LENGTH_SHORT).show());

        final String downloadUrl = convertDriveUrl(driveUrl);

        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(downloadUrl).build();
        client.newCall(request).enqueue(new Callback() { // SỬ DỤNG enqueue()
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
                    // DÙNG BUFFER TỐI ƯU
                    byte[] buffer = new byte[OPTIMIZED_BUFFER_SIZE];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }

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
                    try {
                        if (fos != null) fos.close();
                        if (is != null) is.close();
                        if (response != null) response.close();
                    } catch (IOException ignored) {}
                }
            }
        });
    }

    // --- Logic: Tải file PDF vào CACHE để đọc tạm thời (SỬ DỤNG OkHttp.enqueue ĐỂ TỐI ƯU LUỒNG) ---
    private Call downloadPdfToCache(String pdfUrl, String fileName, PdfSetupCallback callback) {

        if (loadingListener != null) {
            loadingListener.showLoading();
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        final String downloadUrl = convertDriveUrl(pdfUrl);
        Request request = new Request.Builder().url(downloadUrl).build();
        final Call downloadCall = client.newCall(request);

        // SỬ DỤNG enqueue() thay vì new Thread().execute()
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

                    // SỬ DỤNG BUFFER TỐI ƯU
                    byte[] buffer = new byte[OPTIMIZED_BUFFER_SIZE];
                    int len;

                    while ((len = is.read(buffer)) != -1) {
                        // Kiểm tra trạng thái hủy
                        if (isActivityDestroyed || call.isCanceled()) {
                            Log.d(TAG, "Đã hủy tải do thoát màn hình hoặc bị hủy thủ công");
                            return;
                        }
                        fos.write(buffer, 0, len);
                    }

                    fos.flush();
                    final File finalPdfFile = pdfFile; // Khắc phục lỗi effectively final
                    runOnUiThread(() -> callback.setup(finalPdfFile));

                } catch (Exception e) {
                    handleDownloadError(e, call);
                } finally {
                    try {
                        if (is != null) is.close();
                        if (fos != null) fos.close();
                        if (response != null) response.close();
                    } catch (IOException ignored) {}

                    if (call.isCanceled() && pdfFile != null) {
                        pdfFile.delete();
                    }

                    hideLoadingOnUi();
                }
            }
        });

        // Trả về Call ngay lập tức để người gọi có thể hủy nó
        return downloadCall;
    }

    // Hàm hỗ trợ xử lý lỗi
    private void handleDownloadError(Exception e, Call call) {
        // Chỉ hiển thị lỗi nếu Activity chưa bị hủy và Call chưa bị hủy
        if (!isActivityDestroyed && !call.isCanceled()) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(context, "Lỗi tải PDF!", Toast.LENGTH_SHORT).show());
        }
    }


    // === Hàm tìm URL PDF từ Firestore (Không thay đổi) === vo hai khong co dung
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

    // === Hàm gọi chính để xác định nguồn tải (Không thay đổi) ===
    public Call loadAndSetupPdf(String episodePdfLink, String pdfPath, String mainStoryTitle,
                                PdfSetupCallback callback, StringConsumer urlConsumer) {
        if (episodePdfLink != null && !episodePdfLink.isEmpty()) {
            urlConsumer.set(episodePdfLink);
            return downloadPdfToCache(episodePdfLink, "temp_episode.pdf", callback);
        } else if (pdfPath != null) {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                callback.setup(pdfFile);
                if (loadingListener != null) {
                    ((ReadActivity)context).runOnUiThread(() -> loadingListener.hideLoading());
                }
                findAndSetCurrentReadUrl(mainStoryTitle, urlConsumer);
            } else {
                Toast.makeText(context, "File PDF không tồn tại, tải lại...", Toast.LENGTH_SHORT).show();
                loadPdfFromFirestore(mainStoryTitle, callback, urlConsumer);
            }
        }
        else {
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
                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            urlConsumer.set(pdfUrl);
                        }
                    }
                });
    }
}