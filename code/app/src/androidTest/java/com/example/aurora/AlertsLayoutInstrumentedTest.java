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

/**
 * Instrumented layout test for the Alerts UI.
 * <p>
 * This test inflates {@code fragment_alerts} without launching an activity
 * and verifies that the default "empty state" is wired correctly:
 * the message view is visible and the alerts container has no children.
 */
@RunWith(AndroidJUnit4.class)
public class AlertsLayoutInstrumentedTest {

    /**
     * Verifies that the alerts fragment shows an empty-state message by default.
     * <p>
     * The test checks that:
     * <ul>
     *     <li>{@code alertsMessage} exists and is visible,</li>
     *     <li>{@code alertsContainer} exists and starts with zero children, and</li>
     *     <li>the empty-state message text is non-empty.</li>
     * </ul>
     * This confirms that the UI communicates clearly when the user has no alerts yet.
     */
    @Test
    public void alertsLayout_showsEmptyStateMessageByDefault() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View root = inflater.inflate(R.layout.fragment_alerts, null);
        assertNotNull(root);

        TextView alertsMessage = root.findViewById(R.id.alertsMessage);
        LinearLayout alertsContainer = root.findViewById(R.id.alertsContainer);
        assertNotNull(alertsMessage);
        assertNotNull(alertsContainer);
        assertEquals(View.VISIBLE, alertsMessage.getVisibility());
        assertEquals(0, alertsContainer.getChildCount());

        CharSequence text = alertsMessage.getText();
        assertNotNull(text);
        assertTrue(text.length() > 0);
    }
}
