package com.zx.filetool.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.zx.filetool.R;
import com.zx.filetool.util.LocaleHelper;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DEFAULT_DIR = "default_dir";
    public static final String DEFAULT_DIR_FALLBACK = "/storage/emulated/0/株馨科技/";

    public static String getDefaultDir(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEFAULT_DIR, DEFAULT_DIR_FALLBACK);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        RadioGroup rgLanguage = findViewById(R.id.rgLanguage);
        RadioButton rbAuto = findViewById(R.id.rbAuto);
        RadioButton rbEn = findViewById(R.id.rbEn);
        RadioButton rbZh = findViewById(R.id.rbZh);
        EditText etDefaultDir = findViewById(R.id.etDefaultDir);
        Button btnSaveDir = findViewById(R.id.btnSaveDir);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedDir = prefs.getString(KEY_DEFAULT_DIR, null);
        if (savedDir == null) {
            savedDir = DEFAULT_DIR_FALLBACK;
            prefs.edit().putString(KEY_DEFAULT_DIR, savedDir).apply();
        }
        etDefaultDir.setText(savedDir);

        String currentLang = LocaleHelper.getLanguage(this);
        switch (currentLang) {
            case LocaleHelper.LANG_EN:
                rbEn.setChecked(true);
                break;
            case LocaleHelper.LANG_ZH:
                rbZh.setChecked(true);
                break;
            default:
                rbAuto.setChecked(true);
                break;
        }

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String lang;
            if (checkedId == R.id.rbEn) {
                lang = LocaleHelper.LANG_EN;
            } else if (checkedId == R.id.rbZh) {
                lang = LocaleHelper.LANG_ZH;
            } else {
                lang = LocaleHelper.LANG_AUTO;
            }

            if (!lang.equals(currentLang)) {
                LocaleHelper.setLocale(this, lang);
                Toast.makeText(this, R.string.lang_restart_hint, Toast.LENGTH_SHORT).show();
                restartApp();
            }
        });

        btnSaveDir.setOnClickListener(v -> {
            String dir = etDefaultDir.getText().toString().trim();
            if (dir.isEmpty()) {
                dir = DEFAULT_DIR_FALLBACK;
                etDefaultDir.setText(dir);
            }
            prefs.edit().putString(KEY_DEFAULT_DIR, dir).apply();
            Toast.makeText(this, R.string.dir_saved, Toast.LENGTH_SHORT).show();
        });
    }

    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        finish();
    }
}
