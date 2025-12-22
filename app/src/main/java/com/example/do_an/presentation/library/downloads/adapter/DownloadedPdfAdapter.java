package com.example.do_an.presentation.library.downloads.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.data.library.local.dao.DownloadedPdfDao;
import com.example.do_an.data.library.local.entity.DownloadedPdfEntity;
import com.example.do_an.presentation.reading.reader.ReadFragment;

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

        // 🔥 Lấy text "Đang cập nhật" từ resource
        final String author = entity.author != null && !entity.author.isEmpty()
                ? entity.author
                : activity.getString(R.string.updating);

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

        // 🔥 Lấy text theo locale
        String authorLabel = activity.getString(R.string.author_label);
        holder.txtPdfAuthor.setText(authorLabel + author);

        // 🔥 Nút Xóa theo ngôn ngữ
        holder.btnDelete.setText(activity.getString(R.string.remove));

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
                        .replace(R.id.fragmentContainer, readFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(activity, activity.getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION)
                return;

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

        TextView txtMessage = dialog.findViewById(R.id.txtMessage); // 👈 thêm dòng này
        TextView btnYes = dialog.findViewById(R.id.btnYes);
        TextView btnNo = dialog.findViewById(R.id.btnNo);

        // 🔥 Set text theo ngôn ngữ
        txtMessage.setText(activity.getString(R.string.delete_confirm_msg)); // 👈 message mới
        btnYes.setText(activity.getString(R.string.delete_confirm_yes));
        btnNo.setText(activity.getString(R.string.delete_confirm_no));

        btnYes.setOnClickListener(v -> {

            boolean deleted = fileToDelete.delete();
            final String fileNameToDelete = fileToDelete.getName();

            new Thread(() -> pdfDao.delete(entityToDelete)).start();

            File jsonFile = new File(
                    fileToDelete.getParent(),
                    fileToDelete.getName().replace(".pdf", ".json"));
            if (jsonFile.exists())
                jsonFile.delete();

            if (deleted) {
                downloadedPdfs.remove(adapterPos);
                notifyItemRemoved(adapterPos);
                Toast.makeText(activity,
                        activity.getString(R.string.deleted_msg) + fileNameToDelete,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity,
                        activity.getString(R.string.cannot_delete) + fileNameToDelete,
                        Toast.LENGTH_SHORT).show();
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
