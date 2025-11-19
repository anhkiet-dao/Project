package com.example.do_an.application;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.do_an.application.ReadActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private boolean isActivityDestroyed = false;

    // Interface để gọi lại hàm setup PdfRenderer trong Controller
    public interface PdfSetupCallback { void setup(File pdfFile); }
    public interface StringConsumer { void set(String value); } // Dùng để cập nhật currentReadUrl

    public interface LoadingListener {
        void showLoading();
        void hideLoading();
    }

    // 2. KHAI BÁO BIẾN LISTENER
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

    // --- Logic: Tải file PDF dùng OkHttp để lưu trữ VĨNH VIỄN (Không cần sửa) ---
    public void downloadPdfWithOkHttp(String driveUrl, String fileName) {
        Toast.makeText(context, "Bắt đầu tải dữ liệu ...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                // Lấy fileId và tạo downloadUrl
                String fileId = driveUrl.split("/d/")[1].split("/")[0];
                String downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;

                OkHttpClient client = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder().url(downloadUrl).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    ((ReadActivity)context).runOnUiThread(() -> Toast.makeText(context, "Không tải được PDF!", Toast.LENGTH_SHORT).show());
                    return;
                }

                InputStream is = response.body().byteStream();

                // Lưu file vào thư mục /Android/data/.../files/PDF
                File pdfDir = new File(context.getExternalFilesDir(null), "PDF");
                if (!pdfDir.exists()) pdfDir.mkdirs();
                File pdfFile = new File(pdfDir, fileName);

                FileOutputStream fos = new FileOutputStream(pdfFile);
                byte[] buffer = new byte[65536]; // 64 KB
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }

                fos.flush();
                fos.close();
                is.close();

                ((ReadActivity)context).runOnUiThread(() ->
                        Toast.makeText(context, "Tải dữ liệu thành công!", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                ((ReadActivity)context).runOnUiThread(() ->
                        Toast.makeText(context, "Lỗi tải PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // --- Logic: Tải file PDF vào CACHE để đọc tạm thời (ĐÃ SỬA) ---
    private Call downloadPdfToCache(String pdfUrl, String fileName, PdfSetupCallback callback) {
        ((ReadActivity)context).runOnUiThread(() ->
                Toast.makeText(context, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show());

        if (loadingListener != null) {
            loadingListener.showLoading();
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        // 1. Tạo Request và Call trên luồng chính
        final String downloadUrl = convertDriveUrl(pdfUrl);
        Request request = new Request.Builder().url(downloadUrl).build();
        final Call downloadCall = client.newCall(request); // <<< downloadCall hiện là effectively final

        new Thread(() -> {
            try {
                // 2. Thực thi Call bên trong luồng nền
                Response response = downloadCall.execute();
                if (!response.isSuccessful()) {
                    ((ReadActivity)context).runOnUiThread(() -> Toast.makeText(context, "Không tải thành công dữ liệu!", Toast.LENGTH_SHORT).show());
                    return;
                }

                InputStream is = response.body().byteStream();
                File cacheDir = new File(context.getCacheDir(), "PDF");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File pdfFile = new File(cacheDir, fileName);

                FileOutputStream fos = new FileOutputStream(pdfFile);
                byte[] buffer = new byte[64 * 1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    // 3. Sử dụng downloadCall (effectively final) để kiểm tra trạng thái hủy
                    if (isActivityDestroyed || downloadCall.isCanceled()) {
                        is.close(); fos.close(); pdfFile.delete();
                        Log.d(TAG, "Đã hủy tải do thoát màn hình");
                        return;
                    }
                    fos.write(buffer, 0, len);
                }

                fos.flush(); fos.close(); is.close(); response.close();

                ((ReadActivity)context).runOnUiThread(() -> callback.setup(pdfFile));

            } catch (Exception e) {
                if (!isActivityDestroyed && !"Canceled".equals(e.getMessage())) {
                    e.printStackTrace();
                    ((ReadActivity)context).runOnUiThread(() -> Toast.makeText(context, "Lỗi tải PDF!", Toast.LENGTH_SHORT).show());
                }
            }
            finally {
                // 2. ẨN BIỂU TƯỢNG ĐANG TẢI (DÙ THÀNH CÔNG, THẤT BẠI HAY BỊ HỦY)
                if (loadingListener != null) {
                    // Sử dụng runOnUiThread vì DownloadManager không chắc chắn ở luồng chính
                    ((ReadActivity)context).runOnUiThread(() -> loadingListener.hideLoading());
                }
            }
        }).start();

        // 4. Trả về Call ngay lập tức
        return downloadCall;
    }

    // === Hàm tìm URL PDF từ Firestore (dùng cho tải lại/link cũ) (Không cần sửa) ===
    private void loadPdfFromFirestore(String storyDocumentId, PdfSetupCallback callback, StringConsumer urlConsumer) {
        db.collection("Truyentranh").document(storyDocumentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String pdfUrl = doc.getString("pdfUrl");
                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            urlConsumer.set(pdfUrl); // Cập nhật URL chính
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

    // === Hàm gọi chính để xác định nguồn tải (Không cần sửa) ===
    public Call loadAndSetupPdf(String episodePdfLink, String pdfPath, String mainStoryTitle,
                                PdfSetupCallback callback, StringConsumer urlConsumer) {
        if (episodePdfLink != null && !episodePdfLink.isEmpty()) {
            urlConsumer.set(episodePdfLink); // Link tập là link đọc chính
            return downloadPdfToCache(episodePdfLink, "temp_episode.pdf", callback);
        } else if (pdfPath != null) {
            // Đọc file local
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                callback.setup(pdfFile);
                // Tìm link PDF gốc (không cần download, chỉ để gán vào currentReadUrl)
                if (loadingListener != null) {
                    // Đảm bảo ẩn trên UI Thread
                    ((ReadActivity)context).runOnUiThread(() -> loadingListener.hideLoading());
                }
                findAndSetCurrentReadUrl(mainStoryTitle, urlConsumer);
            } else {
                Toast.makeText(context, "File PDF không tồn tại, tải lại...", Toast.LENGTH_SHORT).show();
                loadPdfFromFirestore(mainStoryTitle, callback, urlConsumer);
            }
        }
        else {
            // Tải link của truyện chính
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

    private String convertDriveUrl(String pdfUrl) {
        if (pdfUrl.contains("drive.google.com/file/d/")) {
            String fileId = pdfUrl.split("/d/")[1].split("/")[0];
            return "https://drive.google.com/uc?export=download&id=" + fileId;
        } else {
            return pdfUrl;
        }
    }
}