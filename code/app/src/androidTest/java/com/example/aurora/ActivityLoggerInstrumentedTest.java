package com.example.aurora;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented test for ActivityLogger Firestore logging.
 * Verifies that Firestore receives a new document when a log method is called.
 */
@RunWith(AndroidJUnit4.class)
public class ActivityLoggerInstrumentedTest {

    private FirebaseFirestore db;
    private CollectionReference logsCollection;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        logsCollection = db.collection("logs");
    }

    @Test
    public void testLogEventCreatedWritesToFirestore() throws Exception {
        // Create a unique event log
        String testEventTitle = "Test Event " + System.currentTimeMillis();
        String testEventId = "event_" + System.currentTimeMillis();

        // Log it using the real logger
        ActivityLogger.logEventCreated(testEventId, testEventTitle);

        // Wait briefly for Firestore to update
        Thread.sleep(2000);

        // Query the "logs" collection for a matching entry
        QuerySnapshot snapshot = Tasks.await(
                logsCollection
                        .whereEqualTo("eventTitle", testEventTitle)
                        .get(),
                10, TimeUnit.SECONDS
        );

        List<DocumentSnapshot> docs = snapshot.getDocuments();
        assertNotNull("Query result should not be null", docs);
        assertTrue("Expected at least one log document", !docs.isEmpty());

        // Check the log contains correct type and title
        DocumentSnapshot logDoc = docs.get(0);
        assertTrue("Log type should be 'event_created'",
                "event_created".equals(logDoc.getString("type")));
        assertTrue("Log message should mention event title",
                logDoc.getString("message").contains(testEventTitle));
    }

    @Test
    public void testLogProfileRemovedWritesToFirestore() throws Exception {
        String email = "testuser" + System.currentTimeMillis() + "@example.com";
        ActivityLogger.logProfileRemoved(email);

        Thread.sleep(2000);

        QuerySnapshot snapshot = Tasks.await(
                logsCollection
                        .whereEqualTo("userEmail", email)
                        .whereEqualTo("type", "profile_removed")
                        .get(),
                10, TimeUnit.SECONDS
        );

        List<DocumentSnapshot> docs = snapshot.getDocuments();
        assertNotNull(docs);
        assertTrue("Profile removal log should exist", !docs.isEmpty());
    }
}
