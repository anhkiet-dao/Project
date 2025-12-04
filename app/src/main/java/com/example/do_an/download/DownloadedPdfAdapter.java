package com.example.do_an.download;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle; // Cần import Bundle
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity; // Cần AppCompatActivity để dùng getSupportFragmentManager

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.example.do_an.R;
// Cần import ReadFragment
import com.example.do_an.main.read.ReadFragment; // <<< Import ReadFragment

import org.json.JSONObject;

public class DownloadedPdfAdapter extends RecyclerView.Adapter<DownloadedPdfAdapter.PdfViewHolder> {

    // Giữ nguyên Activity, nhưng chúng ta sẽ coi nó là AppCompatActivity
    private Activity activity;
    private List<File> pdfFiles;

    public DownloadedPdfAdapter(Activity activity, List<File> pdfFiles) {
        this.activity = activity;
        this.pdfFiles = new ArrayList<>(pdfFiles);
    }

    @Override
    public PdfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pdf_item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PdfViewHolder holder, int position) {

        File pdf = pdfFiles.get(position);
        holder.txtPdfName.setText(pdf.getName());

        holder.itemView.setOnClickListener(v -> {
            // === LOGIC CHUYỂN TỪ ACTIVITY SANG FRAGMENT ===

            // 1. Chuẩn bị dữ liệu Bundle
            Bundle readArgs = new Bundle();

            String title = pdf.getName().replace(".pdf", "");
            String storyId = title + "_id";
            String author = "Tác giả ẩn danh";
            String category = "PDF đã tải";
            String imageUrl = "";
            String description = "Đây là file PDF đã tải về.";

            File jsonFile = new File(pdf.getParent(), title + ".json");
            if (jsonFile.exists()) {
                try {
                    FileReader reader = new FileReader(jsonFile);
                    char[] buffer = new char[(int) jsonFile.length()];
                    reader.read(buffer);
                    reader.close();
                    String content = new String(buffer);
                    JSONObject json = new JSONObject(content);

                    storyId = json.optString("STORY_ID", storyId);
                    title = json.optString("STORY_TITLE", title);
                    author = json.optString("STORY_AUTHOR", author);
                    category = json.optString("STORY_CATEGORY", category);
                    imageUrl = json.optString("STORY_IMAGE_URL", imageUrl);
                    description = json.optString("STORY_DESCRIPTION", description);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Đặt dữ liệu vào Bundle
            readArgs.putString("STORY_ID", storyId);
            readArgs.putString("STORY_TITLE", title);
            readArgs.putString("STORY_AUTHOR", author);
            readArgs.putString("STORY_CATEGORY", category);
            readArgs.putString("STORY_IMAGE_URL", imageUrl);
            readArgs.putString("STORY_DESCRIPTION", description);
            // KEY QUAN TRỌNG: Đường dẫn file cục bộ
            readArgs.putString("PDF_PATH", pdf.getAbsolutePath());
            // TAP (Episode title) sẽ là title

            // 2. Kiểm tra Activity và thực hiện Fragment Transaction
            if (activity instanceof AppCompatActivity) {
                AppCompatActivity appCompatActivity = (AppCompatActivity) activity;

                // Tạo instance của ReadFragment
                ReadFragment readFragment = ReadFragment.newInstance(readArgs);
                // Hoặc ReadFragment readFragment = new ReadFragment(); readFragment.setArguments(readArgs);

                appCompatActivity.getSupportFragmentManager()
                        .beginTransaction()
                        // !!! QUAN TRỌNG: Thay R.id.fragment_container bằng ID container thực tế
                        .replace(R.id.fragment_container, readFragment)
                        .addToBackStack(null) // Cho phép nhấn nút back để quay lại màn hình danh sách đã tải
                        .commit();
            } else {
                Toast.makeText(activity, "Lỗi: Không thể mở màn hình đọc (Activity không tương thích)", Toast.LENGTH_SHORT).show();
            }
            // === KẾT THÚC LOGIC CHUYỂN ĐỔI ===
        });

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;
            File fileToDelete = pdfFiles.get(adapterPos);
            showDeleteDialog(fileToDelete, adapterPos, holder);
        });
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }


    private void showDeleteDialog(File fileToDelete, int adapterPos, PdfViewHolder holder) {

        Dialog dialog = new Dialog(this.activity);
        dialog.setContentView(R.layout.note_item_confirm_delete);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView btnYes = dialog.findViewById(R.id.btnYes);
        TextView btnNo = dialog.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {

            boolean deleted = fileToDelete.delete();

            File jsonFile = new File(
                    fileToDelete.getParent(),
                    fileToDelete.getName().replace(".pdf", ".json")
            );
            if (jsonFile.exists()) jsonFile.delete();

            if (deleted) {
                pdfFiles.remove(adapterPos);
                notifyItemRemoved(adapterPos);
                Toast.makeText(this.activity, "Đã xóa " + fileToDelete.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this.activity, "Không thể xóa " + fileToDelete.getName(), Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView txtPdfName, btnDelete;

        public PdfViewHolder(View itemView) {
            super(itemView);
            txtPdfName = itemView.findViewById(R.id.txtPdfName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}