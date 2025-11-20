package com.example.do_an.application;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.example.do_an.R;

import org.json.JSONObject;

public class DownloadedPdfAdapter extends RecyclerView.Adapter<DownloadedPdfAdapter.PdfViewHolder> {

    // ĐÃ SỬA VÀ DUY TRÌ: Thay Context bằng Activity
    private Activity activity;
    // GIỮ NGUYÊN kiểu List<File> ở đây để tương thích, nhưng đảm bảo nó là ArrayList
    private List<File> pdfFiles;

    // ĐÃ SỬA: Cập nhật constructor nhận Activity và tạo bản sao ArrayList
    public DownloadedPdfAdapter(Activity activity, List<File> pdfFiles) {
        this.activity = activity;

        // ********* ĐIỂM SỬA QUAN TRỌNG NHẤT *********
        // Nếu danh sách được truyền vào không phải là ArrayList,
        // nó có thể là danh sách cố định (Immutable List).
        // Chúng ta tạo bản sao sang ArrayList để hỗ trợ thao tác .remove()
        this.pdfFiles = new ArrayList<>(pdfFiles);
        // ********************************************
    }

    @Override
    public PdfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Nên dùng parent.getContext() để lấy Context chính xác cho LayoutInflater
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PdfViewHolder holder, int position) {

        File pdf = pdfFiles.get(position);
        holder.txtPdfName.setText(pdf.getName());

        holder.itemView.setOnClickListener(v -> {
            // Dùng activity thay vì context
            Intent intent = new Intent(activity, ReadActivity.class);

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

            intent.putExtra("STORY_ID", storyId);
            intent.putExtra("STORY_TITLE", title);
            intent.putExtra("STORY_AUTHOR", author);
            intent.putExtra("STORY_CATEGORY", category);
            intent.putExtra("STORY_IMAGE_URL", imageUrl);
            intent.putExtra("STORY_DESCRIPTION", description);
            intent.putExtra("PDF_PATH", pdf.getAbsolutePath());

            // Dùng activity thay vì context
            activity.startActivity(intent);
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

        // Dùng activity đã lưu, không cần kiểm tra lại
        Dialog dialog = new Dialog(this.activity);
        dialog.setContentView(R.layout.item_confirm_delete);
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
                // Thao tác xóa .remove() giờ đã hoạt động an toàn
                pdfFiles.remove(adapterPos);
                notifyItemRemoved(adapterPos);
                // Dùng activity để hiển thị Toast
                Toast.makeText(this.activity, "Đã xóa " + fileToDelete.getName(), Toast.LENGTH_SHORT).show();
            } else {
                // Dùng activity để hiển thị Toast
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