package com.example.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class EventDetailsLayoutInstrumentedTest {

    @Test
    public void eventDetailsLayout_hasAllKeyViews() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the activity_event_details layout without starting the Activity
        View root = inflater.inflate(R.layout.activity_event_details, null);
        assertNotNull(root);

        // Banner image
        assertNotNull(root.findViewById(R.id.imgBanner));

        // Basic event info
        assertNotNull(root.findViewById(R.id.txtTitle));
        assertNotNull(root.findViewById(R.id.txtLocation));
        assertNotNull(root.findViewById(R.id.txtTime));
        assertNotNull(root.findViewById(R.id.txtRegWindow));
        assertNotNull(root.findViewById(R.id.txtStats));

        // About section
        assertNotNull(root.findViewById(R.id.txtAbout));

        // Action buttons
        assertNotNull(root.findViewById(R.id.btnSignUp));
        assertNotNull(root.findViewById(R.id.btnCriteria));
        assertNotNull(root.findViewById(R.id.btnShowQr));
    }
}
