package com.example.do_an.Download;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.do_an.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.example.do_an.data.AppDatabase;
import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity;

public class DownloadFragment extends Fragment{
    private RecyclerView rvDownloadedPdfs;
    private DownloadedPdfAdapter adapter;
    private List<File> pdfFiles;
    private DownloadedPdfDao pdfDao;

    public DownloadFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.download_activity_downloaded_list, container, false);

        rvDownloadedPdfs = view.findViewById(R.id.rvDownloadedPdfs);
        rvDownloadedPdfs.setLayoutManager(new LinearLayoutManager(getContext()));

        pdfDao = AppDatabase.getDatabase(requireContext()).downloadedPdfDao();

        loadDownloadedPdfs();

        return view;
    }

    private void loadDownloadedPdfs() {
        new Thread(() -> {

            List<DownloadedPdfEntity> downloadedEntities = getAllDownloadedPdfs(pdfDao);
            List<File> localFiles = new ArrayList<>();
            List<DownloadedPdfEntity> entitiesToRemove = new ArrayList<>();

            for (DownloadedPdfEntity entity : downloadedEntities) {
                File pdfFile = new File(entity.localFilePath);
                if (pdfFile.exists()) {
                    localFiles.add(pdfFile);
                } else {
                    entitiesToRemove.add(entity);
                }
            }

            if (!entitiesToRemove.isEmpty()) {
                for (DownloadedPdfEntity entity : entitiesToRemove) {
                    pdfDao.delete(entity);
                }
            }

            requireActivity().runOnUiThread(() -> {
                pdfFiles = localFiles;

                if (pdfFiles.isEmpty()) {
                    Toast.makeText(getContext(), "Chưa có file PDF nào đã tải xuống!", Toast.LENGTH_SHORT).show();
                }

                adapter = new DownloadedPdfAdapter(requireActivity(), pdfFiles, pdfDao);
                rvDownloadedPdfs.setAdapter(adapter);
            });
        }).start();
    }

    private List<DownloadedPdfEntity> getAllDownloadedPdfs(DownloadedPdfDao dao) {

        try {
            return dao.getAllPdfs();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}