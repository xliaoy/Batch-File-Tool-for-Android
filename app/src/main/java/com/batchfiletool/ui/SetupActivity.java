package com.zx.filetool.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.zx.filetool.MainActivity;
import com.zx.filetool.R;
import com.zx.filetool.util.LocaleHelper;

public class SetupActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_SETUP_DONE = "setup_done";
    private static final String KEY_DEFAULT_DIR = "default_dir";

    private View pagePermission, pageDirectory, pageLanguage, pageWelcome;
    private Button btnNext, btnPrev;
    private LinearLayout dotIndicator;
    private ImageView[] dots = new ImageView[4];
    private int currentPage = 0;

    private EditText etSetupDir;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isSetupDone()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_setup);

        pagePermission = findViewById(R.id.pagePermission);
        pageDirectory = findViewById(R.id.pageDirectory);
        pageLanguage = findViewById(R.id.pageLanguage);
        pageWelcome = findViewById(R.id.pageWelcome);

        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        dotIndicator = findViewById(R.id.dotIndicator);

        etSetupDir = findViewById(R.id.etSetupDir);
        etSetupDir.setText(SettingsActivity.getDefaultDir(this));

        createDots();
        setupPermissionPage();
        setupLanguagePage();
        setupWelcomePage();

        btnNext.setOnClickListener(v -> {
            if (currentPage < 3) {
                if (currentPage == 0) {
                    if (!isPermissionGranted()) {
                        requestPermission();
                        return;
                    }
                }
                if (currentPage == 1) {
                    saveDirectory();
                }
                if (currentPage == 2) {
                    saveLanguage();
                }
                currentPage++;
                showPage(currentPage);
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                showPage(currentPage);
            }
        });

        showPage(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentPage == 0 && isPermissionGranted()) {
            showPermissionGranted();
        }
    }

    private void setupPermissionPage() {
        Button btnGrant = findViewById(R.id.btnGrantPermission);
        btnGrant.setOnClickListener(v -> requestPermission());
    }

    private void setupLanguagePage() {
        RadioGroup rgLang = findViewById(R.id.rgSetupLang);
        String lang = LocaleHelper.getLanguage(this);
        if (LocaleHelper.LANG_EN.equals(lang)) {
            ((RadioButton) findViewById(R.id.rbSetupEn)).setChecked(true);
        } else if (LocaleHelper.LANG_ZH.equals(lang)) {
            ((RadioButton) findViewById(R.id.rbSetupZh)).setChecked(true);
        }
    }

    private void setupWelcomePage() {
        Button btnStart = findViewById(R.id.btnStartApp);
        btnStart.setOnClickListener(v -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_SETUP_DONE, true).apply();
            goToMain();
        });
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ignored) {}
        }
    }

    private boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    private void showPermissionGranted() {
        TextView tvStatus = findViewById(R.id.tvPermStatus);
        tvStatus.setText(R.string.permission_granted_status);
        tvStatus.setVisibility(View.VISIBLE);
        findViewById(R.id.btnGrantPermission).setEnabled(false);
        btnNext.setText(R.string.next);
    }

    private void saveDirectory() {
        String dir = etSetupDir.getText().toString().trim();
        if (dir.isEmpty()) {
            dir = SettingsActivity.DEFAULT_DIR_FALLBACK;
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString(KEY_DEFAULT_DIR, dir).apply();
    }

    private void saveLanguage() {
        RadioGroup rgLang = findViewById(R.id.rgSetupLang);
        int id = rgLang.getCheckedRadioButtonId();
        String lang;
        if (id == R.id.rbSetupEn) {
            lang = LocaleHelper.LANG_EN;
        } else if (id == R.id.rbSetupZh) {
            lang = LocaleHelper.LANG_ZH;
        } else {
            lang = LocaleHelper.LANG_AUTO;
        }
        LocaleHelper.setLocale(this, lang);
    }

    private void showPage(int page) {
        pagePermission.setVisibility(page == 0 ? View.VISIBLE : View.GONE);
        pageDirectory.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        pageLanguage.setVisibility(page == 2 ? View.VISIBLE : View.GONE);
        pageWelcome.setVisibility(page == 3 ? View.VISIBLE : View.GONE);

        btnPrev.setVisibility(page == 0 ? View.GONE : View.VISIBLE);

        if (page == 3) {
            btnNext.setVisibility(View.GONE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
            if (page == 0) {
                if (isPermissionGranted()) {
                    showPermissionGranted();
                    btnNext.setText(R.string.next);
                } else {
                    btnNext.setText(R.string.grant_permission);
                }
            } else {
                btnNext.setText(R.string.next);
            }
        }
        updateDots(page);
        currentPage = page;
    }

    private void createDots() {
        for (int i = 0; i < 4; i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
            params.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(i == 0 ? R.drawable.terminal_dot_green : R.drawable.terminal_dot_yellow);
            dots[i] = dot;
            dotIndicator.addView(dot);
        }
    }

    private void updateDots(int page) {
        for (int i = 0; i < 4; i++) {
            dots[i].setImageResource(i <= page ? R.drawable.terminal_dot_green : R.drawable.terminal_dot_yellow);
        }
    }

    private boolean isSetupDone() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_SETUP_DONE, false);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
