package com.example.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented layout test for the notification settings dialog.
 * <p>
 * This test inflates {@code dialog_notification_settings} without launching
 * an activity and verifies that the descriptive text and the toggle switch
 * for enabling/disabling notifications are present and correctly labelled.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationSettingsDialogInstrumentedTest {

    /**
     * Verifies that the notification settings dialog contains a non-empty
     * description and a switch clearly labelled for notifications.
     * <p>
     * The test asserts that:
     * <ul>
     *     <li>The description TextView exists and has non-empty text, and</li>
     *     <li>The {@code switchNotifications} control exists and its label
     *         contains the word "notifications".</li>
     * </ul>
     * This ensures that entrants can understand and control their
     * notification preferences from the dialog.
     */
    @Test
    public void notificationSettingsDialog_hasDescriptionAndSwitch() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View root = inflater.inflate(R.layout.dialog_notification_settings, null);
        assertNotNull(root);

        TextView notificationDesc = root.findViewById(R.id.notificationDesc);
        SwitchCompat switchNotifications = root.findViewById(R.id.switchNotifications);

        assertNotNull(notificationDesc);
        assertNotNull(switchNotifications);

        CharSequence descText = notificationDesc.getText();
        assertNotNull(descText);
        assertTrue(descText.length() > 0);

        CharSequence switchText = switchNotifications.getText();
        assertNotNull(switchText);
        assertTrue(switchText.toString().toLowerCase().contains("notifications"));
    }
}
