package com.example.do_an.Download;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileReader; // Không cần thiết nữa, nhưng giữ lại nếu cần
import java.util.ArrayList;
import java.util.List;

import com.example.do_an.R;
import com.example.do_an.UI.ReadFragment;
import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity; // Cần import Entity

import org.json.JSONObject; // Không cần thiết nữa, nhưng giữ lại nếu cần

public class DownloadedPdfAdapter extends RecyclerView.Adapter<DownloadedPdfAdapter.PdfViewHolder> {

    private Activity activity;
    private List<File> pdfFiles;
    private DownloadedPdfDao pdfDao;

    public DownloadedPdfAdapter(Activity activity, List<File> pdfFiles, DownloadedPdfDao pdfDao) {
        this.activity = activity;
        this.pdfFiles = new ArrayList<>(pdfFiles);
        this.pdfDao = pdfDao;
    }

    @Override
    public PdfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pdf_item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PdfViewHolder holder, int position) {

        File pdf = pdfFiles.get(position);

        final String pdfFileName = pdf.getName();

        String defaultTitle = pdfFileName.replace(".pdf", "");
        holder.txtPdfName.setText(defaultTitle);
        holder.txtPdfAuthor.setText("Tác giả: Tác giả ẩn danh (Đang tải...)");

        new Thread(() -> {
            DownloadedPdfEntity entity = pdfDao.getPdfByFileName(pdfFileName);

            activity.runOnUiThread(() -> {
                String title;
                String author;

                if (entity != null) {
                    title = entity.fileName.replace(".pdf", "");
                    author = entity.author != null && !entity.author.isEmpty() ? entity.author : "Đang cập nhật";
                } else {
                    title = defaultTitle;
                    author = "Không tìm thấy";
                    Toast.makeText(activity, "Lỗi: Không tìm thấy thông tin Room cho " + defaultTitle, Toast.LENGTH_SHORT).show();
                }

                holder.txtPdfName.setText(title);
                holder.txtPdfAuthor.setText("Tác giả: " + author);

                final String finalTitle = title;
                final String finalAuthor = author;

                holder.itemView.setOnClickListener(v -> {
                    Bundle readArgs = new Bundle();

                    readArgs.putString("STORY_TITLE", finalTitle);
                    readArgs.putString("STORY_AUTHOR", finalAuthor);
                    readArgs.putString("PDF_PATH", pdf.getAbsolutePath());

                    if (activity instanceof AppCompatActivity) {
                        AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
                        ReadFragment readFragment = ReadFragment.newInstance(readArgs);

                        appCompatActivity.getSupportFragmentManager()
                                .beginTransaction()
                                .add(R.id.fragment_container, readFragment)
                                .addToBackStack(null)
                                .commit();
                    } else {
                        Toast.makeText(activity, "Không thể mở file!", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }).start();

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;
            File fileToDelete = pdfFiles.get(adapterPos);
            showDeleteDialog(fileToDelete, adapterPos);
        });
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }

    private void showDeleteDialog(File fileToDelete, int adapterPos) {
        Dialog dialog = new Dialog(this.activity);
        dialog.setContentView(R.layout.note_item_confirm_delete);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView btnYes = dialog.findViewById(R.id.btnYes);
        TextView btnNo = dialog.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {

            boolean deleted = fileToDelete.delete();
            final String fileNameToDelete = fileToDelete.getName();

            new Thread(() -> {
                DownloadedPdfEntity entity = pdfDao.getPdfByFileName(fileNameToDelete);
                if (entity != null) pdfDao.delete(entity);
            }).start();

            File jsonFile = new File(
                    fileToDelete.getParent(),
                    fileToDelete.getName().replace(".pdf", ".json")
            );
            if (jsonFile.exists()) jsonFile.delete();

            if (deleted) {
                pdfFiles.remove(adapterPos);
                notifyItemRemoved(adapterPos);
                Toast.makeText(this.activity, "Đã xóa " + fileNameToDelete, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this.activity, "Không thể xóa " + fileNameToDelete, Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView txtPdfName, txtPdfAuthor, btnDelete;

        public PdfViewHolder(View itemView) {
            super(itemView);
            txtPdfName = itemView.findViewById(R.id.txtPdfName);
            txtPdfAuthor = itemView.findViewById(R.id.txtPdfAuthor); // LẤY VIEW TÁC GIẢ
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}