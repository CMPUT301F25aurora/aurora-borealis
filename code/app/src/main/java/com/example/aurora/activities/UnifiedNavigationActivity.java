package com.example.aurora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aurora.R;
import com.example.aurora.fragments.EventsFragment;
import com.example.aurora.fragments.ProfileFragment;
import com.example.aurora.fragments.AlertsFragment;
import com.example.aurora.fragments.OrganizerEventsFragment;  // NEW fragment

public class UnifiedNavigationActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private Button tabHome, tabProfile, tabAlerts;
    private String mode; // entrant or organizer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_navigation_pager);

        mode = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_mode", "entrant");

        pager = findViewById(R.id.pager);
        tabHome = findViewById(R.id.navEvents);
        tabProfile = findViewById(R.id.navProfile);
        tabAlerts = findViewById(R.id.navAlerts);

        pager.setAdapter(new PagerAdapter(this, mode));
        pager.setUserInputEnabled(true);
        pager.setCurrentItem(0, false);
        highlight(0);

        tabHome.setOnClickListener(v -> pager.setCurrentItem(0, true));
        tabProfile.setOnClickListener(v -> pager.setCurrentItem(1, true));
        tabAlerts.setOnClickListener(v -> pager.setCurrentItem(2, true));

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { highlight(position); }
        });
    }

    private void highlight(int i) {
        float on = 1f, off = 0.5f;
        tabHome.setAlpha(i==0?on:off);
        tabProfile.setAlpha(i==1?on:off);
        tabAlerts.setAlpha(i==2?on:off);
    }

    static class PagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {

        private final String mode;

        public PagerAdapter(@NonNull AppCompatActivity fa, String mode) {
            super(fa);
            this.mode = mode;
        }

        @NonNull @Override
        public androidx.fragment.app.Fragment createFragment(int position) {

            if (position == 0) {
                if ("organizer".equalsIgnoreCase(mode)) {
                    return new OrganizerEventsFragment();
                }
                return new EventsFragment();
            }

            if (position == 1) return new ProfileFragment();
            return new AlertsFragment();
        }

        @Override public int getItemCount() { return 3; }
    }
}
