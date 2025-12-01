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

@RunWith(AndroidJUnit4.class)
public class NotificationSettingsDialogInstrumentedTest {

    @Test
    public void notificationSettingsDialog_hasDescriptionAndSwitch() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the dialog_notification_settings layout
        View root = inflater.inflate(R.layout.dialog_notification_settings, null);
        assertNotNull(root);

        TextView notificationDesc = root.findViewById(R.id.notificationDesc);
        SwitchCompat switchNotifications = root.findViewById(R.id.switchNotifications);

        // Views should exist
        assertNotNull(notificationDesc);
        assertNotNull(switchNotifications);

        // Description text should not be empty
        CharSequence descText = notificationDesc.getText();
        assertNotNull(descText);
        assertTrue(descText.length() > 0);

        // Switch should have the correct label
        CharSequence switchText = switchNotifications.getText();
        assertNotNull(switchText);
        assertTrue(switchText.toString().toLowerCase().contains("notifications"));

        // By default, we don't assert checked/unchecked because
        // that is up to your app logic – we just ensure the UI is present.
    }
}
