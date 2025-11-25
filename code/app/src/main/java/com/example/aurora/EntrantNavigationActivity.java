/*
 * References:
 *
 * 1) Android Developers — "Slide between fragments using ViewPager2"
 *    https://developer.android.com/develop/ui/views/animations/screen-slide-2
 *    Used as a reference for wiring ViewPager2 with FragmentStateAdapter and swiping between fragment pages.
 *
 * 2) Tutorialwing — "ViewPager2 With Fragment and FragmentStateAdapter"
 *    https://tutorialwing.com/viewpager2-with-fragment-and-fragmentstateadapter/
 *    Used as a reference for implementing a FragmentStateAdapter subclass that returns different fragments by position.
 */

package com.example.aurora;

import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
/**
 * Navigation activity for entrants in the Aurora app.
 *
 * Switches between three main sections using ViewPager2:
 * - Events
 * - Profile
 * - Alerts
 *
 * Each section is shown as a separate fragment to keep the interface
 * organized and easy to navigate.
 */


public class EntrantNavigationActivity extends BaseActivity {

    private ViewPager2 pager;
    private Button tabEvents, tabProfile, tabAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_navigation_pager);

        pager = findViewById(R.id.pager);
        tabEvents = findViewById(R.id.navEvents);
        tabProfile = findViewById(R.id.navProfile);
        tabAlerts = findViewById(R.id.navAlerts);

        pager.setAdapter(new PagerAdapter(this));
        pager.setUserInputEnabled(true);
        pager.setCurrentItem(0, false);
        highlight(0);

        tabEvents.setOnClickListener(v -> pager.setCurrentItem(0, true));
        tabProfile.setOnClickListener(v -> pager.setCurrentItem(1, true));
        tabAlerts.setOnClickListener(v -> pager.setCurrentItem(2, true));

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { highlight(position); }
        });
    }

    private void highlight(int i) {
        float on = 1f, off = 0.5f;
        tabEvents.setAlpha(i==0?on:off);
        tabProfile.setAlpha(i==1?on:off);
        tabAlerts.setAlpha(i==2?on:off);
    }

    static class PagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public PagerAdapter(@NonNull AppCompatActivity fa) { super(fa); }
        @NonNull @Override public androidx.fragment.app.Fragment createFragment(int position) {
            if (position == 0) return new EventsFragment();
            if (position == 1) return new ProfileFragment();
            return new AlertsFragment();
        }
        @Override public int getItemCount() { return 3; }
    }
}
