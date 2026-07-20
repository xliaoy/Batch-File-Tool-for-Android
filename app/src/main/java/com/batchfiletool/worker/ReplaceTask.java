package com.zx.filetool.worker;

import android.os.AsyncTask;
import com.zx.filetool.util.FileUtil;
import com.zx.filetool.util.LocaleHelper;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ReplaceTask extends AsyncTask<Void, ReplaceTask.Progress, ReplaceTask.Result> {

    public interface Callback {
        void onProgressUpdate(Progress progress);
        void onComplete(Result result);
    }

    public static class Progress {
        public String message;
        public int percent;
        public int scanned;
        public int modified;
        public int skipped;
        public int errors;
        public Progress(String message, int percent, int scanned, int modified, int skipped, int errors) {
            this.message = message;
            this.percent = percent;
            this.scanned = scanned;
            this.modified = modified;
            this.skipped = skipped;
            this.errors = errors;
        }
    }

    public static class Result {
        public boolean success;
        public int totalFiles;
        public int modifiedFiles;
        public int skippedFiles;
        public int errorFiles;
        public String summary;
    }

    private final File rootDir;
    private final String searchText;
    private final String replaceText;
    private final boolean useRegex;
    private final boolean caseSensitive;
    private final boolean createBackup;
    private final WeakReference<Callback> callbackRef;

    public ReplaceTask(File rootDir, String searchText, String replaceText,
                       boolean useRegex, boolean caseSensitive, boolean createBackup,
                       Callback callback) {
        this.rootDir = rootDir;
        this.searchText = searchText;
        this.replaceText = replaceText;
        this.useRegex = useRegex;
        this.caseSensitive = caseSensitive;
        this.createBackup = createBackup;
        this.callbackRef = new WeakReference<>(callback);
    }

    @Override
    protected Result doInBackground(Void... voids) {
        Result result = new Result();
        result.success = true;
        result.totalFiles = 0;
        result.modifiedFiles = 0;
        result.skippedFiles = 0;
        result.errorFiles = 0;

        FileUtil.walkFiles(rootDir, f -> result.totalFiles++);
        String scanning = LocaleHelper.logPrefix("scanning");
        publishProgress(new Progress("[" + scanning + "] " + result.totalFiles + " files found.",
                0, 0, 0, 0, 0));

        final int[] processed = {0};
        final int[] modified = {0};
        final int[] skipped = {0};
        final int[] errors = {0};

        FileUtil.walkFiles(rootDir, file -> {
            if (isCancelled()) return;

            processed[0]++;

            if (FileUtil.isBinaryFile(file)) {
                skipped[0]++;
                publishProgress(new Progress(
                        "[" + LocaleHelper.logPrefix("skip_binary") + "] " + file.getAbsolutePath(),
                        (int) ((double) processed[0] / result.totalFiles * 100),
                        processed[0], modified[0], skipped[0], errors[0]));
                return;
            }

            try {
                String content = FileUtil.readFileContent(file, Charset.forName("UTF-8"));
                String newContent;

                if (useRegex) {
                    int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                    newContent = Pattern.compile(searchText, flags)
                            .matcher(content).replaceAll(replaceText);
                } else if (caseSensitive) {
                    newContent = content.replace(searchText, replaceText);
                } else {
                    newContent = content.replaceAll(
                            "(?i)" + Pattern.quote(searchText),
                            replaceText.replace("\\", "\\\\").replace("$", "\\$"));
                }

                if (!newContent.equals(content)) {
                    if (createBackup) {
                        File backupFile = FileUtil.createBackup(file);
                        publishProgress(new Progress(
                                "[" + LocaleHelper.logPrefix("backup") + "] " + backupFile.getName(),
                                (int) ((double) processed[0] / result.totalFiles * 100),
                                processed[0], modified[0], skipped[0], errors[0]));
                    }
                    FileUtil.writeFileContent(file, newContent, Charset.forName("UTF-8"));
                    modified[0]++;
                    publishProgress(new Progress(
                            "[" + LocaleHelper.logPrefix("replaced") + "] " + file.getAbsolutePath(),
                            (int) ((double) processed[0] / result.totalFiles * 100),
                            processed[0], modified[0], skipped[0], errors[0]));
                } else {
                    publishProgress(new Progress(
                            "[" + LocaleHelper.logPrefix("unchanged") + "] " + file.getAbsolutePath(),
                            (int) ((double) processed[0] / result.totalFiles * 100),
                            processed[0], modified[0], skipped[0], errors[0]));
                }
            } catch (IOException e) {
                errors[0]++;
                publishProgress(new Progress(
                        "[" + LocaleHelper.logPrefix("error") + "] " + file.getAbsolutePath() + ": " + e.getMessage(),
                        (int) ((double) processed[0] / result.totalFiles * 100),
                        processed[0], modified[0], skipped[0], errors[0]));
            } catch (PatternSyntaxException e) {
                errors[0]++;
                publishProgress(new Progress(
                        "[" + LocaleHelper.logPrefix("error") + "] " + e.getMessage(),
                        (int) ((double) processed[0] / result.totalFiles * 100),
                        processed[0], modified[0], skipped[0], errors[0]));
                cancel(true);
            }
        });

        result.modifiedFiles = modified[0];
        result.skippedFiles = skipped[0];
        result.errorFiles = errors[0];

        String done = LocaleHelper.logPrefix("done");
        String successLog = LocaleHelper.logPrefix("success_modified");
        if (isCancelled()) {
            result.summary = "[" + LocaleHelper.logPrefix("cancelled_log") + "] " + processed[0] + " files.";
        } else {
            result.summary = "[" + done + "] " + result.modifiedFiles + "/" + result.totalFiles + " files.";
            publishProgress(new Progress(
                    "[" + successLog + "] " + result.modifiedFiles + " file(s) modified.",
                    100, processed[0], modified[0], skipped[0], errors[0]));
        }
        return result;
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        Callback cb = callbackRef.get();
        if (cb != null && values.length > 0) {
            cb.onProgressUpdate(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        Callback cb = callbackRef.get();
        if (cb != null) {
            cb.onComplete(result);
        }
    }

    @Override
    protected void onCancelled(Result result) {
        if (result == null) result = new Result();
        result.summary = "[" + LocaleHelper.logPrefix("cancelled_log") + "]";
        Callback cb = callbackRef.get();
        if (cb != null) {
            cb.onComplete(result);
        }
    }
}
