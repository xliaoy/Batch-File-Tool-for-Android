package com.zx.filetool.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * 文件操作工具类
 */
public class FileUtil {

    public static boolean isBinaryFile(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int bytesRead = in.read(buffer);
            if (bytesRead <= 0) return false;
            for (int i = 0; i < bytesRead; i++) {
                if (buffer[i] == 0) return true;
            }
            return false;
        } catch (IOException e) {
            return true;
        } finally {
            closeQuietly(in);
        }
    }

    public static String readFileContent(File file, Charset charset) throws IOException {
        if (charset == null) charset = Charset.forName("UTF-8");
        byte[] bytes = readAllBytes(file);
        return new String(bytes, charset);
    }

    public static void writeFileContent(File file, String content, Charset charset) throws IOException {
        writeAllBytes(file, content.getBytes(charset));
    }

    public static File createBackup(File file) throws IOException {
        File backupFile = new File(file.getParentFile(), file.getName() + ".bak");
        int counter = 1;
        while (backupFile.exists()) {
            backupFile = new File(file.getParentFile(), file.getName() + ".bak." + counter);
            counter++;
        }
        copyFile(file, backupFile);
        return backupFile;
    }

    public static boolean matchesAnyPattern(String fileName, List<String> patterns) {
        for (String pattern : patterns) {
            if (matchesGlob(fileName, pattern.trim())) return true;
        }
        return false;
    }

    private static boolean matchesGlob(String fileName, String pattern) {
        if (pattern.isEmpty()) return false;
        String regex = globToRegex(pattern);
        try {
            return fileName.matches(regex);
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    static String globToRegex(String pattern) {
        StringBuilder sb = new StringBuilder();
        if (!pattern.contains("*") && !pattern.contains("?")) {
            sb.append(".*");
            appendEscaped(sb, pattern);
            sb.append(".*");
        } else {
            sb.append(".*");
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                switch (c) {
                    case '*': sb.append(".*"); break;
                    case '?': sb.append("."); break;
                    case '.': case '[': case ']': case '{': case '}':
                    case '(': case ')': case '^': case '$': case '|':
                    case '+': case '\\':
                        sb.append("\\").append(c); break;
                    default: sb.append(c); break;
                }
            }
            sb.append(".*");
        }
        return sb.toString();
    }

    private static void appendEscaped(StringBuilder sb, String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (".*?+[]{}()^$|\\".indexOf(c) >= 0) {
                sb.append("\\");
            }
            sb.append(c);
        }
    }

    public interface FileVisitor {
        void onFile(File file);
    }

    public static void walkFiles(File rootDir, FileVisitor visitor) {
        File[] children = rootDir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                walkFiles(child, visitor);
            } else if (child.isFile()) {
                visitor.onFile(child);
            }
        }
    }

    private static byte[] readAllBytes(File file) throws IOException {
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } finally {
            closeQuietly(fis);
            closeQuietly(bos);
        }
    }

    private static void writeAllBytes(File file, byte[] data) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
        } finally {
            closeQuietly(fos);
        }
    }

    private static void copyFile(File src, File dest) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);
            inChannel = fis.getChannel();
            outChannel = fos.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            closeQuietly(outChannel);
            closeQuietly(inChannel);
            closeQuietly(fos);
            closeQuietly(fis);
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }
}
