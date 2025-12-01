package com.example.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class OrganizerNotificationsLayoutInstrumentedTest {

    @Test
    public void organizerNotificationsLayout_hasHeaderAndContainer() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the activity_organizer_notifications layout
        View root = inflater.inflate(R.layout.activity_organizer_notifications, null);
        assertNotNull(root);

        LinearLayout headerContainer = root.findViewById(R.id.headerContainer);
        LinearLayout notificationsContainer = root.findViewById(R.id.notificationsContainer);

        // Both main sections should exist
        assertNotNull(headerContainer);
        assertNotNull(notificationsContainer);

        // Header should contain at least one child view (title/buttons/etc.)
        assertTrue(headerContainer.getChildCount() > 0);

        // Notifications container should start empty (no cards yet)
        assertEquals(0, notificationsContainer.getChildCount());
    }
}
