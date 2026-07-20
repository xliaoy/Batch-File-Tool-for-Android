package com.zx.filetool;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.zx.filetool.ui.DeleteFragment;
import com.zx.filetool.ui.ReplaceFragment;
import com.zx.filetool.ui.SettingsActivity;
import com.zx.filetool.util.LocaleHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new MainPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.tab_replace);
            } else {
                tab.setText(R.string.tab_delete);
            }
        }).attach();

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {

        public MainPagerAdapter(AppCompatActivity activity) {
            super(activity);
        }

        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            if (position == 0) {
                return new ReplaceFragment();
            } else {
                return new DeleteFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
