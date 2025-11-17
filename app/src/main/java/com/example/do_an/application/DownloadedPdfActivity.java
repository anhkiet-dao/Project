package com.example.do_an.application;

import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.example.do_an.R;

public class DownloadedPdfActivity extends AppCompatActivity {

    private RecyclerView rvDownloadedPdfs;
    private DownloadedPdfAdapter adapter;
    private List<File> pdfFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded_list);

        rvDownloadedPdfs = findViewById(R.id.rvDownloadedPdfs);
        rvDownloadedPdfs.setLayoutManager(new LinearLayoutManager(this));

        loadDownloadedPdfs();
    }

    private void loadDownloadedPdfs() {
        File pdfDir = new File(getExternalFilesDir(null), "PDF");
        if (!pdfDir.exists() || !pdfDir.isDirectory()) {
            Toast.makeText(this, "Chưa có file PDF nào!", Toast.LENGTH_SHORT).show();
            pdfFiles = new ArrayList<>();
        } else {
            File[] files = pdfDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files != null) {
                pdfFiles = Arrays.asList(files);
            } else {
                pdfFiles = new ArrayList<>();
            }
        }

        adapter = new DownloadedPdfAdapter(this, pdfFiles);
        rvDownloadedPdfs.setAdapter(adapter);
    }
}

