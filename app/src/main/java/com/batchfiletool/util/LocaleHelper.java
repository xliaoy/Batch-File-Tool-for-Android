package com.zx.filetool.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_LANGUAGE = "language";

    public static final String LANG_AUTO = "auto";
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";

    public static Context setLocale(Context context) {
        String lang = getLanguage(context);
        return updateResources(context, lang);
    }

    public static Context setLocale(Context context, String language) {
        saveLanguage(context, language);
        return updateResources(context, language);
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_AUTO);
    }

    public static boolean isChinese() {
        Locale locale = Locale.getDefault();
        return locale.getLanguage().equals(Locale.SIMPLIFIED_CHINESE.getLanguage());
    }

    private static void saveLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    private static Context updateResources(Context context, String language) {
        Locale locale;
        if (LANG_AUTO.equals(language)) {
            locale = Resources.getSystem().getConfiguration().getLocales().get(0);
        } else if (LANG_ZH.equals(language)) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else {
            locale = Locale.ENGLISH;
        }

        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    public static String logPrefix(String key) {
        if (isChinese()) {
            switch (key) {
                case "scanning": return "扫描中";
                case "scan_done": return "预览完成";
                case "skip_binary": return "跳过(二进制)";
                case "replaced": return "已替换";
                case "unchanged": return "未更改";
                case "backup": return "已备份";
                case "matched": return "匹配";
                case "deleted": return "已删除";
                case "failed": return "失败";
                case "error": return "错误";
                case "cancelled_log": return "已取消";
                case "done": return "完成";
                case "success_modified": return "已修改成功";
                case "success_deleted": return "已删除成功";
                default: return key;
            }
        }
        switch (key) {
            case "scanning": return "Scanning";
            case "scan_done": return "Preview complete";
            case "skip_binary": return "SKIP(Binary)";
            case "replaced": return "REPLACED";
            case "unchanged": return "UNCHANGED";
            case "backup": return "BACKUP";
            case "matched": return "MATCH";
            case "deleted": return "DELETED";
            case "failed": return "FAILED";
            case "error": return "ERROR";
            case "cancelled_log": return "CANCELLED";
            case "done": return "Done";
            case "success_modified": return "Modified successfully";
            case "success_deleted": return "Deleted successfully";
            default: return key;
        }
    }
}
