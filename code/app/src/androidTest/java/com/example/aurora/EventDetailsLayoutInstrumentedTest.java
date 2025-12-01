package com.example.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

/**
 * Instrumented layout test for the entrant-facing event details screen.
 * <p>
 * This test inflates {@code activity_event_details} in isolation (without
 * launching the Activity) and verifies that all of the expected key views
 * are present in the layout. It focuses on structural correctness of the
 * XML rather than runtime behaviour.
 */
@RunWith(AndroidJUnit4.class)
public class EventDetailsLayoutInstrumentedTest {

    /**
     * Verifies that the event details layout declares all key UI elements.
     * <p>
     * The test checks for:
     * <ul>
     *     <li>Banner image for the event poster,</li>
     *     <li>Basic event info views (title, location, time, registration window, stats),</li>
     *     <li>About/description text view, and</li>
     *     <li>Action buttons (Sign Up, Criteria, Show QR).</li>
     * </ul>
     * Ensuring these views exist helps guarantee that
     * {@code EventDetailsActivity} has the widgets it needs to bind data
     * and support the entrant flows.
     */
    @Test
    public void eventDetailsLayout_hasAllKeyViews() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View root = inflater.inflate(R.layout.activity_event_details, null);
        assertNotNull(root);

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
