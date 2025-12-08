package com.example.do_an.Download;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import com.example.do_an.R;
import com.example.do_an.data.AppDatabase;
import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends Fragment {
    private static final String TAG = "DownloadFragment";
    private RecyclerView rvDownloadedPdfs;
    private TextView tvNoDownloads;
    private DownloadedPdfAdapter adapter;
    private List<DownloadedPdfEntity> downloadedPdfs;
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
        loadDownloadedPdfs();

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
            Log.d(TAG, "Bước 1: Số lượng Entities lấy từ DAO (Bao gồm Cache): " + entities.size());

            List<DownloadedPdfEntity> validEntities = new ArrayList<>();
            List<DownloadedPdfEntity> entitiesToRemove = new ArrayList<>();

            for (DownloadedPdfEntity entity : entities) {
                File pdfFile = new File(entity.localFilePath);

                Log.d(TAG, "Kiểm tra Entity: " + entity.fileName + " | Path: " + entity.localFilePath + " | isCache: " + entity.isCache);

                if (pdfFile.exists()) {
                    validEntities.add(entity);
                    Log.d(TAG, " -> HỢP LỆ: File vật lý tồn tại.");

                } else {
                    entitiesToRemove.add(entity);
                    Log.w(TAG, " -> KHÔNG HỢP LỆ: File vật lý bị mất. Sẽ xóa khỏi Room.");
                }
            }

            Log.d(TAG, "Bước 2: Số lượng Entities hợp lệ để hiển thị: " + validEntities.size());

            if (!entitiesToRemove.isEmpty()) {
                Log.w(TAG, "Đang xóa " + entitiesToRemove.size() + " records lỗi khỏi Room.");
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

                    if (adapter == null) {
                        adapter = new DownloadedPdfAdapter(requireActivity(), downloadedPdfs, pdfDao);
                        rvDownloadedPdfs.setAdapter(adapter);
                    } else {
                        adapter.setPdfList(downloadedPdfs);
                    }
                }
            });

        }).start();
    }

    private List<DownloadedPdfEntity> getAllDownloadedPdfs(DownloadedPdfDao dao) {
        try {
            return dao.getAllPdfs();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lấy PDFs từ DAO", e);
            return new ArrayList<>();
        }
    }
}