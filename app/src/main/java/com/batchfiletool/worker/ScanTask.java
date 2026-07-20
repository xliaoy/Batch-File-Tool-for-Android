package com.zx.filetool.worker;

import android.os.AsyncTask;
import com.zx.filetool.util.FileUtil;
import com.zx.filetool.util.LocaleHelper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ScanTask extends AsyncTask<Void, String, List<File>> {

    public interface Callback {
        void onProgress(String message);
        void onComplete(List<File> matchedFiles);
    }

    private final File rootDir;
    private final List<String> patterns;
    private final WeakReference<Callback> callbackRef;

    public ScanTask(File rootDir, List<String> patterns, Callback callback) {
        this.rootDir = rootDir;
        this.patterns = patterns;
        this.callbackRef = new WeakReference<>(callback);
    }

    @Override
    protected List<File> doInBackground(Void... voids) {
        List<File> matchedFiles = new ArrayList<>();
        String scanning = LocaleHelper.logPrefix("scanning");
        publishProgress("[" + scanning + "] " + rootDir.getAbsolutePath());

        FileUtil.walkFiles(rootDir, file -> {
            if (isCancelled()) return;
            if (FileUtil.matchesAnyPattern(file.getName(), patterns)) {
                matchedFiles.add(file);
                String matched = LocaleHelper.logPrefix("matched");
                publishProgress("[" + matched + "] " + file.getAbsolutePath());
            }
        });

        String scanDone = LocaleHelper.logPrefix("scan_done");
        publishProgress("[" + scanDone + "] " + matchedFiles.size() + " file(s).");
        return matchedFiles;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        Callback cb = callbackRef.get();
        if (cb != null && values.length > 0) {
            cb.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(List<File> files) {
        Callback cb = callbackRef.get();
        if (cb != null) {
            cb.onComplete(files);
        }
    }

    @Override
    protected void onCancelled(List<File> files) {
        Callback cb = callbackRef.get();
        if (cb != null) {
            cb.onComplete(files != null ? files : new ArrayList<File>());
        }
    }
}
