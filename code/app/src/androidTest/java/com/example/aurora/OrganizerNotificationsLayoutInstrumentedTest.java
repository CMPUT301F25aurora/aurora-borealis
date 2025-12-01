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

/**
 * Instrumented layout test for the organizer notifications screen.
 * <p>
 * This test inflates {@code activity_organizer_notifications} in isolation
 * and verifies that the header and notifications container sections are
 * present and structurally correct.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerNotificationsLayoutInstrumentedTest {

    /**
     * Verifies that the organizer notifications layout declares both a
     * header section and an initially empty notifications container.
     * <p>
     * The test asserts that:
     * <ul>
     *     <li>{@code headerContainer} exists and contains at least one child
     *         (such as a title or filter controls), and</li>
     *     <li>{@code notificationsContainer} exists and starts with zero
     *         child views, indicating that no notification cards are shown
     *         by default.</li>
     * </ul>
     * This ensures the organizer screen is ready to display notifications
     * while showing a clean state when there are none.
     */
    @Test
    public void organizerNotificationsLayout_hasHeaderAndContainer() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View root = inflater.inflate(R.layout.activity_organizer_notifications, null);
        assertNotNull(root);

        LinearLayout headerContainer = root.findViewById(R.id.headerContainer);
        LinearLayout notificationsContainer = root.findViewById(R.id.notificationsContainer);

        assertNotNull(headerContainer);
        assertNotNull(notificationsContainer);

        assertTrue(headerContainer.getChildCount() > 0);

        assertEquals(0, notificationsContainer.getChildCount());
    }
}
