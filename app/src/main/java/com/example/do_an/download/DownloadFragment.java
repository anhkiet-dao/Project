package com.example.do_an.download;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ctrl + alt + o

public class DownloadFragment extends Fragment{
    private RecyclerView rvDownloadedPdfs;
    private DownloadedPdfAdapter adapter;
    private List<File> pdfFiles;

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Gắn layout (giống Activity nhưng dùng inflater)
        View view = inflater.inflate(R.layout.download_activity_downloaded_list, container, false);

        rvDownloadedPdfs = view.findViewById(R.id.rvDownloadedPdfs);
        rvDownloadedPdfs.setLayoutManager(new LinearLayoutManager(getContext()));

        loadDownloadedPdfs();

        return view;
    }

    private void loadDownloadedPdfs() {

        // getContext() thay cho this trong Activity
        File pdfDir = new File(requireContext().getExternalFilesDir(null), "PDF");

        if (!pdfDir.exists() || !pdfDir.isDirectory()) {
            Toast.makeText(getContext(), "Chưa có file PDF nào!", Toast.LENGTH_SHORT).show();
            pdfFiles = new ArrayList<>();
        } else {
            File[] files = pdfDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files != null) {
                pdfFiles = Arrays.asList(files);
            } else {
                pdfFiles = new ArrayList<>();
            }
        }

        adapter = new DownloadedPdfAdapter(requireActivity(), pdfFiles);
        rvDownloadedPdfs.setAdapter(adapter);
    }
}
