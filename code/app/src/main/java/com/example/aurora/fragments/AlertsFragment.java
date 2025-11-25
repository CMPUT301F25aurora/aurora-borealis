/**
 * AlertsFragment.java
 *
 * This fragment is responsible for displaying the alerts section of the Aurora app.
 * It inflates the layout defined in fragment_alerts.xml when the fragment’s view is created.
 * Fragments like this are used to modularize the UI, allowing the alerts screen to be
 * reused within different activities or navigation components.
 */

package com.example.aurora.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.aurora.R;

public class AlertsFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }
}