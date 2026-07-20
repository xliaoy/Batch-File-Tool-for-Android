package com.zx.filetool.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zx.filetool.R;
import com.zx.filetool.worker.DeleteTask;
import com.zx.filetool.worker.ScanTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeleteFragment extends Fragment {

    private EditText etDirPath, etPatterns;
    private Button btnPreview, btnDelete, btnStop, btnBrowse;
    private TextView tvLog, tvStatus, tvFileCount;
    private TextView tvStatScanned, tvStatDeleted, tvStatFailed, tvStatTotal;
    private LinearLayout statsBar;
    private ProgressBar progressBar;
    private ScrollView scrollLog;
    private List<File> pendingFiles;
    private ScanTask scanTask;
    private DeleteTask deleteTask;
    private ActivityResultLauncher<Uri> dirPickerLauncher;

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dirPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(), this::onDirectorySelected);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delete, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        etDirPath = view.findViewById(R.id.etDirPath);
        etPatterns = view.findViewById(R.id.etPatterns);
        btnPreview = view.findViewById(R.id.btnPreview);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnStop = view.findViewById(R.id.btnStop);
        btnBrowse = view.findViewById(R.id.btnBrowse);
        tvLog = view.findViewById(R.id.tvLog);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvFileCount = view.findViewById(R.id.tvFileCount);
        progressBar = view.findViewById(R.id.progressBar);
        scrollLog = view.findViewById(R.id.scrollLog);
        statsBar = view.findViewById(R.id.statsBar);
        tvStatScanned = view.findViewById(R.id.tvStatScanned);
        tvStatDeleted = view.findViewById(R.id.tvStatDeleted);
        tvStatFailed = view.findViewById(R.id.tvStatFailed);
        tvStatTotal = view.findViewById(R.id.tvStatTotal);
        pendingFiles = new ArrayList<>();
        tvFileCount.setText(getString(R.string.files_found, 0));

        btnBrowse.setOnClickListener(v -> openDirectoryPicker());
        btnPreview.setOnClickListener(v -> previewDelete());
        btnDelete.setOnClickListener(v -> executeDelete());
        btnStop.setOnClickListener(v -> stopOperation());

        etDirPath.setText(SettingsActivity.getDefaultDir(requireContext()));
    }

    private void openDirectoryPicker() {
        dirPickerLauncher.launch(null);
    }

    private void onDirectorySelected(Uri treeUri) {
        if (treeUri != null) {
            requireActivity().getContentResolver().takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            String path = uriToFilePath(treeUri);
            etDirPath.setText(path != null ? path : treeUri.toString());
        }
    }

    private String uriToFilePath(Uri uri) {
        String docId;
        try {
            docId = DocumentsContract.getTreeDocumentId(uri);
        } catch (Exception e) {
            return null;
        }
        String[] parts = docId.split(":");
        if (parts.length >= 2) {
            String type = parts[0];
            String path = parts[1];
            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path;
            }
            return "/storage/" + type + "/" + path;
        }
        return null;
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                tvStatus.setText("Storage permission required");
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
                    startActivity(intent);
                } catch (Exception ignored) {}
                return false;
            }
        }
        return true;
    }

    private void previewDelete() {
        String dirPath = etDirPath.getText().toString().trim();
        List<String> patterns = getPatterns();

        if (dirPath.isEmpty()) {
            tvStatus.setText(R.string.select_dir_first);
            return;
        }
        File rootDir = new File(dirPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            tvStatus.setText(R.string.dir_not_exist);
            return;
        }
        if (patterns.isEmpty()) {
            tvStatus.setText(R.string.enter_pattern);
            return;
        }
        if (!checkPermission()) return;

        tvLog.setText("");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        tvFileCount.setText("Scanning...");
        tvStatus.setText("Scanning...");
        statsBar.setVisibility(View.VISIBLE);
        resetStats();
        pendingFiles.clear();
        btnDelete.setEnabled(false);
        setUiEnabled(false);

        scanTask = new ScanTask(rootDir, patterns, new ScanTask.Callback() {
            @Override
            public void onProgress(String message) {
                appendLog(message);
            }
            @Override
            public void onComplete(List<File> matchedFiles) {
                pendingFiles = matchedFiles;
                tvStatScanned.setText(String.valueOf(matchedFiles.size()));
                tvStatTotal.setText(String.valueOf(matchedFiles.size()));
                progressBar.setVisibility(View.GONE);
                tvFileCount.setText(getString(R.string.files_found, matchedFiles.size()));
                tvStatus.setText("Preview complete: " + matchedFiles.size() + " file(s).");
                setUiEnabled(true);
                btnDelete.setEnabled(!matchedFiles.isEmpty());
                scanTask = null;

                if (matchedFiles.isEmpty()) {
                    appendLog(getString(R.string.no_files_matched));
                }
            }
        });
        scanTask.execute();
    }

    private void executeDelete() {
        if (pendingFiles.isEmpty()) {
            tvStatus.setText(R.string.no_files_to_delete);
            return;
        }

        StringBuilder sb = new StringBuilder();
        int maxShow = Math.min(pendingFiles.size(), 15);
        for (int i = 0; i < maxShow; i++) {
            sb.append(pendingFiles.get(i).getName()).append("\n");
        }
        if (pendingFiles.size() > maxShow) {
            sb.append("... and ").append(pendingFiles.size() - maxShow).append(" more file(s)");
        }

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_message, pendingFiles.size())
                        + "\n\n" + sb.toString())
                .setPositiveButton(R.string.delete, (dialog, which) -> performDelete())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performDelete() {
        List<File> filesToDelete = new ArrayList<>(pendingFiles);
        tvLog.setText("");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressBar.setIndeterminate(false);
        tvStatus.setText("Deleting...");
        statsBar.setVisibility(View.VISIBLE);
        resetStats();
        tvStatTotal.setText(String.valueOf(filesToDelete.size()));
        setUiEnabled(false);

        deleteTask = new DeleteTask(filesToDelete, new DeleteTask.Callback() {
            @Override
            public void onProgress(DeleteTask.Progress p) {
                appendLog(p.message);
                progressBar.setProgress(p.percent);
                tvStatScanned.setText(String.valueOf(p.deleted + p.failed));
                tvStatDeleted.setText(String.valueOf(p.deleted));
                tvStatFailed.setText(String.valueOf(p.failed));
            }
            @Override
            public void onComplete(DeleteTask.Result result) {
                if (result != null) {
                    appendLog(result.summary);
                }
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("Completed");
                tvFileCount.setText(getString(R.string.files_found, 0));
                btnDelete.setEnabled(false);
                pendingFiles.clear();
                setUiEnabled(true);
                deleteTask = null;
            }
        });
        deleteTask.execute();
    }

    private void stopOperation() {
        if (scanTask != null && !scanTask.isCancelled()) {
            scanTask.cancel(true);
            setUiEnabled(true);
        }
        if (deleteTask != null && !deleteTask.isCancelled()) {
            deleteTask.cancel(true);
            setUiEnabled(true);
        }
        progressBar.setVisibility(View.GONE);
        tvStatus.setText(R.string.cancelled);
    }

    private void setUiEnabled(boolean enabled) {
        etDirPath.setEnabled(enabled);
        etPatterns.setEnabled(enabled);
        btnPreview.setEnabled(enabled);
        btnStop.setEnabled(!enabled);
        btnBrowse.setEnabled(enabled);
    }

    private void resetStats() {
        tvStatScanned.setText("0");
        tvStatDeleted.setText("0");
        tvStatFailed.setText("0");
        tvStatTotal.setText("0");
    }

    private List<String> getPatterns() {
        List<String> patterns = new ArrayList<>();
        String text = etPatterns.getText().toString().trim();
        if (text.isEmpty()) return patterns;
        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) patterns.add(trimmed);
        }
        return patterns;
    }

    private void appendLog(String message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            String timestamp = TIME_FMT.format(new Date());
            String existing = tvLog.getText().toString();
            if (existing.length() > 0) {
                tvLog.setText(existing + "\n" + "[" + timestamp + "] " + message);
            } else {
                tvLog.setText("[" + timestamp + "] " + message);
            }
            scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
        });
    }
}
