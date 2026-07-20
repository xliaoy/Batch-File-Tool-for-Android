package com.zx.filetool.worker;

import android.os.AsyncTask;
import com.zx.filetool.util.LocaleHelper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

public class DeleteTask extends AsyncTask<Void, DeleteTask.Progress, DeleteTask.Result> {

    public interface Callback {
        void onProgress(Progress progress);
        void onComplete(Result result);
    }

    public static class Progress {
        public String message;
        public int percent;
        public int deleted;
        public int failed;
        public Progress(String message, int percent, int deleted, int failed) {
            this.message = message;
            this.percent = percent;
            this.deleted = deleted;
            this.failed = failed;
        }
    }

    public static class Result {
        public int total;
        public int deleted;
        public int failed;
        public String summary;
    }

    private final List<File> files;
    private final WeakReference<Callback> callbackRef;

    public DeleteTask(List<File> files, Callback callback) {
        this.files = files;
        this.callbackRef = new WeakReference<>(callback);
    }

    @Override
    protected Result doInBackground(Void... voids) {
        Result result = new Result();
        result.total = files.size();
        result.deleted = 0;
        result.failed = 0;

        int deleted = 0;
        int failed = 0;

        for (int i = 0; i < files.size(); i++) {
            if (isCancelled()) break;
            File f = files.get(i);
            boolean ok = f.delete();
            if (ok) {
                deleted++;
                String label = LocaleHelper.logPrefix("deleted");
                publishProgress(new Progress(
                        "[" + label + "] " + f.getAbsolutePath(),
                        (int) ((double) (i + 1) / result.total * 100),
                        deleted, failed));
            } else {
                failed++;
                String label = LocaleHelper.logPrefix("failed");
                publishProgress(new Progress(
                        "[" + label + "] " + f.getAbsolutePath(),
                        (int) ((double) (i + 1) / result.total * 100),
                        deleted, failed));
            }
        }

        result.deleted = deleted;
        result.failed = failed;

        String done = LocaleHelper.logPrefix("done");
        String success = LocaleHelper.logPrefix("success_deleted");
        if (isCancelled()) {
            result.summary = "[" + LocaleHelper.logPrefix("cancelled_log") + "] " + deleted + " files.";
        } else {
            result.summary = "[" + done + "] " + deleted + "/" + result.total + " files.";
            publishProgress(new Progress(
                    "[" + success + "] " + deleted + " file(s) deleted.",
                    100, deleted, failed));
        }
        return result;
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        Callback cb = callbackRef.get();
        if (cb != null && values.length > 0) {
            cb.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        Callback cb = callbackRef.get();
        if (cb != null) cb.onComplete(result);
    }

    @Override
    protected void onCancelled(Result result) {
        if (result == null) result = new Result();
        result.summary = "[" + LocaleHelper.logPrefix("cancelled_log") + "]";
        Callback cb = callbackRef.get();
        if (cb != null) cb.onComplete(result);
    }
}
