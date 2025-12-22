package com.example.do_an.presentation.library.downloads;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.core.database.AppDatabase;
import com.example.do_an.data.library.local.dao.DownloadedPdfDao;
import com.example.do_an.data.library.local.entity.DownloadedPdfEntity;
import com.example.do_an.presentation.library.downloads.adapter.DownloadedPdfAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends Fragment {
    private static final String TAG = "DownloadFragment";

    private RecyclerView recyclerDownloads;
    private TextView textNoDownloads;
    private DownloadedPdfAdapter adapter;
    private List<DownloadedPdfEntity> downloadedPdfs;
    private DownloadedPdfDao pdfDao;
    private boolean isViewCreated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.download_fragment_downloaded_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;
        pdfDao = AppDatabase.getDatabase(requireContext()).downloadedPdfDao();
        setupViews(view);
        loadViews();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pdfDao != null && isViewCreated) {
            fetchAndDisplayDownloads();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
    }

    private void setupViews(View view) {
        recyclerDownloads = view.findViewById(R.id.recyclerDownloads);
        textNoDownloads = view.findViewById(R.id.textNoDownloads);

        recyclerDownloads.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void loadViews() {
        fetchAndDisplayDownloads();
    }

    private void setupListeners() {
        // No listeners needed for this fragment
    }

    private void fetchAndDisplayDownloads() {
        if (!isViewCreated || getContext() == null)
            return;

        new Thread(() -> {
            List<DownloadedPdfEntity> entities = getAllDownloadedPdfs();
            List<DownloadedPdfEntity> validEntities = validateAndCleanupFiles(entities);

            if (isViewCreated && isAdded()) {
                requireActivity().runOnUiThread(() -> updateUI(validEntities));
            }
        }).start();
    }

    private List<DownloadedPdfEntity> getAllDownloadedPdfs() {
        try {
            return pdfDao.getAllPdfs();
        } catch (Exception e) {
            Log.e(TAG, "Error loading PDFs", e);
            return new ArrayList<>();
        }
    }

    private List<DownloadedPdfEntity> validateAndCleanupFiles(List<DownloadedPdfEntity> entities) {
        List<DownloadedPdfEntity> validEntities = new ArrayList<>();
        List<DownloadedPdfEntity> entitiesToRemove = new ArrayList<>();

        for (DownloadedPdfEntity entity : entities) {
            if (isFileExists(entity.localFilePath)) {
                validEntities.add(entity);
            } else {
                entitiesToRemove.add(entity);
            }
        }

        removeInvalidEntities(entitiesToRemove);
        return validEntities;
    }

    private boolean isFileExists(String filePath) {
        return new File(filePath).exists();
    }

    private void removeInvalidEntities(List<DownloadedPdfEntity> entities) {
        if (!entities.isEmpty()) {
            for (DownloadedPdfEntity entity : entities) {
                pdfDao.delete(entity);
            }
        }
    }

    private void updateUI(List<DownloadedPdfEntity> validEntities) {
        if (!isViewCreated)
            return;

        downloadedPdfs = validEntities;

        if (downloadedPdfs.isEmpty()) {
            showEmptyState();
        } else {
            showDownloadsList();
        }
    }

    private void showEmptyState() {
        recyclerDownloads.setVisibility(View.GONE);
        textNoDownloads.setVisibility(View.VISIBLE);
        textNoDownloads.setText(getString(R.string.no_downloads));
    }

    private void showDownloadsList() {
        recyclerDownloads.setVisibility(View.VISIBLE);
        textNoDownloads.setVisibility(View.GONE);

        if (adapter == null) {
            adapter = new DownloadedPdfAdapter(requireActivity(), downloadedPdfs, pdfDao);
            recyclerDownloads.setAdapter(adapter);
        } else {
            adapter.setPdfList(downloadedPdfs);
        }
    }
}
