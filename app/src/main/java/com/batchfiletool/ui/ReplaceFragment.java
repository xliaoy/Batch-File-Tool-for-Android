package com.zx.filetool.ui;

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
import com.zx.filetool.worker.ReplaceTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReplaceFragment extends Fragment {

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private EditText etDirPath, etSearch, etReplace;
    private CheckBox cbRegex, cbCaseSensitive, cbBackup;
    private Button btnStart, btnStop, btnBrowse;
    private TextView tvLog, tvStatus;
    private TextView tvStatScanned, tvStatModified, tvStatSkipped, tvStatErrors;
    private LinearLayout statsBar;
    private ProgressBar progressBar;
    private ScrollView scrollLog;
    private ReplaceTask currentTask;
    private ActivityResultLauncher<Uri> dirPickerLauncher;

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
        View view = inflater.inflate(R.layout.fragment_replace, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        etDirPath = view.findViewById(R.id.etDirPath);
        etSearch = view.findViewById(R.id.etSearch);
        etReplace = view.findViewById(R.id.etReplace);
        cbRegex = view.findViewById(R.id.cbRegex);
        cbCaseSensitive = view.findViewById(R.id.cbCaseSensitive);
        cbBackup = view.findViewById(R.id.cbBackup);
        btnStart = view.findViewById(R.id.btnStart);
        btnStop = view.findViewById(R.id.btnStop);
        btnBrowse = view.findViewById(R.id.btnBrowse);
        tvLog = view.findViewById(R.id.tvLog);
        tvStatus = view.findViewById(R.id.tvStatus);
        progressBar = view.findViewById(R.id.progressBar);
        scrollLog = view.findViewById(R.id.scrollLog);
        statsBar = view.findViewById(R.id.statsBar);
        tvStatScanned = view.findViewById(R.id.tvStatScanned);
        tvStatModified = view.findViewById(R.id.tvStatModified);
        tvStatSkipped = view.findViewById(R.id.tvStatSkipped);
        tvStatErrors = view.findViewById(R.id.tvStatErrors);

        btnBrowse.setOnClickListener(v -> openDirectoryPicker());
        btnStart.setOnClickListener(v -> startReplace());
        btnStop.setOnClickListener(v -> stopReplace());

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

    private void startReplace() {
        String dirPath = etDirPath.getText().toString().trim();
        String search = etSearch.getText().toString();
        String replace = etReplace.getText().toString();

        if (dirPath.isEmpty()) {
            tvStatus.setText(R.string.select_dir_first);
            return;
        }
        File rootDir = new File(dirPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            tvStatus.setText(R.string.dir_not_exist);
            return;
        }
        if (search.isEmpty()) {
            tvStatus.setText(R.string.enter_search_text);
            return;
        }
        if (!checkPermission()) return;

        tvLog.setText("");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        tvStatus.setText(R.string.processing);
        statsBar.setVisibility(View.VISIBLE);
        resetStats();
        setUiEnabled(false);

        currentTask = new ReplaceTask(rootDir, search, replace,
                cbRegex.isChecked(), cbCaseSensitive.isChecked(), cbBackup.isChecked(),
                new ReplaceTask.Callback() {
                    @Override
                    public void onProgressUpdate(ReplaceTask.Progress p) {
                        appendLog(p.message);
                        progressBar.setProgress(p.percent);
                        tvStatScanned.setText(String.valueOf(p.scanned));
                        tvStatModified.setText(String.valueOf(p.modified));
                        tvStatSkipped.setText(String.valueOf(p.skipped));
                        tvStatErrors.setText(String.valueOf(p.errors));
                    }
                    @Override
                    public void onComplete(ReplaceTask.Result result) {
                        if (result != null) {
                            appendLog(result.summary);
                        }
                        tvStatus.setText(R.string.completed);
                        progressBar.setVisibility(View.GONE);
                        setUiEnabled(true);
                        currentTask = null;
                    }
                });
        currentTask.execute();
    }

    private void stopReplace() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(true);
            tvStatus.setText(R.string.cancelled);
            progressBar.setVisibility(View.GONE);
            setUiEnabled(true);
        }
    }

    private void setUiEnabled(boolean enabled) {
        etDirPath.setEnabled(enabled);
        etSearch.setEnabled(enabled);
        etReplace.setEnabled(enabled);
        cbRegex.setEnabled(enabled);
        cbCaseSensitive.setEnabled(enabled);
        cbBackup.setEnabled(enabled);
        btnStart.setEnabled(enabled);
        btnStop.setEnabled(!enabled);
        btnBrowse.setEnabled(enabled);
    }

    private void resetStats() {
        tvStatScanned.setText("0");
        tvStatModified.setText("0");
        tvStatSkipped.setText("0");
        tvStatErrors.setText("0");
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
