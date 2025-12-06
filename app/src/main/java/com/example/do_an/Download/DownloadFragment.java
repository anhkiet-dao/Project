package com.example.do_an.Download;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.do_an.R;
import com.example.do_an.data.AppDatabase;
import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends Fragment {
    private RecyclerView rvDownloadedPdfs;
    private TextView tvNoDownloads;
    private DownloadedPdfAdapter adapter;
    // ĐỔI TỪ List<File> sang List<DownloadedPdfEntity>
    private List<DownloadedPdfEntity> downloadedPdfs; // ⬅️ THAY ĐỔI
    private DownloadedPdfDao pdfDao;

    public DownloadFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.download_activity_downloaded_list, container, false);

        rvDownloadedPdfs = view.findViewById(R.id.rvDownloadedPdfs);
        rvDownloadedPdfs.setLayoutManager(new LinearLayoutManager(getContext()));

        tvNoDownloads = view.findViewById(R.id.tvNoDownloads);

        pdfDao = AppDatabase.getDatabase(getContext()).downloadedPdfDao();
        loadDownloadedPdfs(); // Gọi hàm tải dữ liệu

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pdfDao != null) {
            loadDownloadedPdfs();
        }
    }

    private void loadDownloadedPdfs() {
        if (getContext() == null) return;

        new Thread(() -> {
            List<DownloadedPdfEntity> entities = getAllDownloadedPdfs(pdfDao);

            List<DownloadedPdfEntity> validEntities = new ArrayList<>(); // List chứa các entity còn file
            List<DownloadedPdfEntity> entitiesToRemove = new ArrayList<>(); // List chứa các entity bị mất file

            for (DownloadedPdfEntity entity : entities) {
                File pdfFile = new File(entity.localFilePath);
                if (pdfFile.exists()) {
                    validEntities.add(entity);
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
                downloadedPdfs = validEntities;

                if (downloadedPdfs.isEmpty()) {
                    rvDownloadedPdfs.setVisibility(View.GONE);
                    tvNoDownloads.setVisibility(View.VISIBLE);
                } else {
                    rvDownloadedPdfs.setVisibility(View.VISIBLE);
                    tvNoDownloads.setVisibility(View.GONE);

                    adapter = new DownloadedPdfAdapter(requireActivity(), downloadedPdfs, pdfDao);
                    rvDownloadedPdfs.setAdapter(adapter);
                }
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