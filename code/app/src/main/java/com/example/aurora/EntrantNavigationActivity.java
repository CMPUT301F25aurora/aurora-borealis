/**
 * EntrantActivity.java
 *
 * This activity represents the main screen for entrants in the Aurora app.
 * It connects to Firestore to retrieve a list of available events that users can view
 * and register for. The events will be displayed in a RecyclerView using a linear layout.
 *
 * Future implementations will handle displaying event details, registration actions,
 * and viewing the entrant’s event history.
 */


package com.example.aurora;

import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class EntrantNavigationActivity extends AppCompatActivity {

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
