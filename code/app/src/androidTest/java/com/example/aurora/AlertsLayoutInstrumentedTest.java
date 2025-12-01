package com.example.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AlertsLayoutInstrumentedTest {

    @Test
    public void alertsLayout_showsEmptyStateMessageByDefault() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate fragment_alerts without launching the Activity
        View root = inflater.inflate(R.layout.fragment_alerts, null);
        assertNotNull(root);

        TextView alertsMessage = root.findViewById(R.id.alertsMessage);
        LinearLayout alertsContainer = root.findViewById(R.id.alertsContainer);

        // Both views should exist
        assertNotNull(alertsMessage);
        assertNotNull(alertsContainer);

        // By default the "no alerts" message should be visible
        assertEquals(View.VISIBLE, alertsMessage.getVisibility());
        // And the container for real alerts should start empty
        assertEquals(0, alertsContainer.getChildCount());

        // Optional: make sure the message text is not blank
        CharSequence text = alertsMessage.getText();
        assertNotNull(text);
        assertTrue(text.length() > 0);
    }
}
