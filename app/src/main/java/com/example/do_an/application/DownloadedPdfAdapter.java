package com.example.do_an.application;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import com.example.do_an.R;
import org.json.JSONObject;

public class DownloadedPdfAdapter extends RecyclerView.Adapter<DownloadedPdfAdapter.PdfViewHolder> {

    private Context context;
    private List<File> pdfFiles;

    public DownloadedPdfAdapter(Context context, List<File> pdfFiles) {
        this.context = context;
        this.pdfFiles = pdfFiles;
    }

    @Override
    public PdfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PdfViewHolder holder, int position) {
        File pdf = pdfFiles.get(position);
        holder.txtPdfName.setText(pdf.getName());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReadActivity.class);

            // 🔹 Mặc định fake info nếu không có file metadata
            String title = pdf.getName().replace(".pdf", "");
            String storyId = title + "_id";
            String author = "Tác giả ẩn danh";
            String category = "PDF đã tải";
            String imageUrl = "";
            String description = "Đây là file PDF đã tải về.";

            // 🔹 Kiểm tra file metadata JSON
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

            // 🔹 Gửi thông tin truyện + đường dẫn file PDF
            intent.putExtra("STORY_ID", storyId);
            intent.putExtra("STORY_TITLE", title);
            intent.putExtra("STORY_AUTHOR", author);
            intent.putExtra("STORY_CATEGORY", category);
            intent.putExtra("STORY_IMAGE_URL", imageUrl);
            intent.putExtra("STORY_DESCRIPTION", description);
            intent.putExtra("PDF_PATH", pdf.getAbsolutePath());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView txtPdfName;

        public PdfViewHolder(View itemView) {
            super(itemView);
            txtPdfName = itemView.findViewById(R.id.txtPdfName);
        }
    }
}
