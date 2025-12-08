package com.example.do_an.Download;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.UI.ReadFragment;
import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class DownloadedPdfAdapter extends RecyclerView.Adapter<DownloadedPdfAdapter.PdfViewHolder> {

    private Activity activity;
    private List<DownloadedPdfEntity> downloadedPdfs;
    private DownloadedPdfDao pdfDao;

    public DownloadedPdfAdapter(Activity activity, List<DownloadedPdfEntity> downloadedPdfs, DownloadedPdfDao pdfDao) {
        this.activity = activity;
        this.downloadedPdfs = new ArrayList<>(downloadedPdfs);
        this.pdfDao = pdfDao;
    }

    public void setPdfList(List<DownloadedPdfEntity> newPdfs) {
        this.downloadedPdfs.clear();
        this.downloadedPdfs.addAll(newPdfs);
        notifyDataSetChanged();
    }

    @Override
    public PdfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PdfViewHolder holder, int position) {

        DownloadedPdfEntity entity = downloadedPdfs.get(position);

        final String title = entity.fileName.replace(".pdf", "");
        final String author = entity.author != null && !entity.author.isEmpty() ? entity.author : "Đang cập nhật";
        final String localFilePath = entity.localFilePath;

        if (entity.coverImageUrl != null && !entity.coverImageUrl.isEmpty()) {
            Glide.with(activity)
                    .load(entity.coverImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.txtPdfName.setText(title);
        holder.txtPdfAuthor.setText("Tác giả: " + author);

        holder.itemView.setOnClickListener(v -> {
            Bundle readArgs = new Bundle();

            readArgs.putString("STORY_TITLE", title);
            readArgs.putString("STORY_AUTHOR", author);
            readArgs.putString("PDF_PATH", localFilePath);
            readArgs.putString("STORY_IMAGE_URL", entity.coverImageUrl);

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

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            File fileToDelete = new File(localFilePath);
            showDeleteDialog(fileToDelete, entity, adapterPos);
        });
    }

    @Override
    public int getItemCount() {
        return downloadedPdfs.size();
    }

    private void showDeleteDialog(File fileToDelete, DownloadedPdfEntity entityToDelete, int adapterPos) {
        Dialog dialog = new Dialog(this.activity);
        dialog.setContentView(R.layout.note_item_confirm_delete);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView btnYes = dialog.findViewById(R.id.btnYes);
        TextView btnNo = dialog.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {

            boolean deleted = fileToDelete.delete();
            final String fileNameToDelete = fileToDelete.getName();

            new Thread(() -> {
                pdfDao.delete(entityToDelete);
            }).start();

            File jsonFile = new File(
                    fileToDelete.getParent(),
                    fileToDelete.getName().replace(".pdf", ".json")
            );
            if (jsonFile.exists()) jsonFile.delete();

            if (deleted) {
                downloadedPdfs.remove(adapterPos);
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
        ImageView imgCover;

        public PdfViewHolder(View itemView) {
            super(itemView);
            txtPdfName = itemView.findViewById(R.id.txtPdfName);
            txtPdfAuthor = itemView.findViewById(R.id.txtPdfAuthor);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            imgCover = itemView.findViewById(R.id.imgCover);
        }
    }
}