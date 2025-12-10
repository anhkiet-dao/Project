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
    private TextView tvNoDownloads, tvTitle;
    private DownloadedPdfAdapter adapter;
    private List<DownloadedPdfEntity> downloadedPdfs;
    private DownloadedPdfDao pdfDao;

    public DownloadFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.download_fragment_downloaded_list, container, false);

        // 📌 Ánh xạ view
        rvDownloadedPdfs = view.findViewById(R.id.rvDownloadedPdfs);
        rvDownloadedPdfs.setLayoutManager(new LinearLayoutManager(getContext()));

        tvNoDownloads = view.findViewById(R.id.tvNoDownloads);
        tvTitle = view.findViewById(R.id.tvTitle);

        // 🌍 Set text đa ngôn ngữ
        tvTitle.setText(getString(R.string.downloaded_list_title));

        // 📌 Load dữ liệu
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

            List<DownloadedPdfEntity> validEntities = new ArrayList<>();
            List<DownloadedPdfEntity> entitiesToRemove = new ArrayList<>();

            // 📌 Kiểm tra file vật lý có còn tồn tại
            for (DownloadedPdfEntity entity : entities) {
                File pdfFile = new File(entity.localFilePath);

                if (pdfFile.exists()) {
                    validEntities.add(entity);
                } else {
                    entitiesToRemove.add(entity);
                }
            }

            // ❌ Xóa dữ liệu rác khỏi Room
            if (!entitiesToRemove.isEmpty()) {
                for (DownloadedPdfEntity entity : entitiesToRemove) {
                    pdfDao.delete(entity);
                }
            }

            // 🔄 Cập nhật giao diện
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    downloadedPdfs = validEntities;

                    if (downloadedPdfs.isEmpty()) {
                        rvDownloadedPdfs.setVisibility(View.GONE);
                        tvNoDownloads.setVisibility(View.VISIBLE);
                        tvNoDownloads.setText(getString(R.string.no_downloads)); // 🔥 đa ngôn ngữ
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
            }

        }).start();
    }

    private List<DownloadedPdfEntity> getAllDownloadedPdfs(DownloadedPdfDao dao) {
        try {
            return dao.getAllPdfs();
        } catch (Exception e) {
            Log.e(TAG, "Error loading PDFs", e);
            return new ArrayList<>();
        }
    }
}
