package com.example.aurora;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aurora.utils.ActivityLogger;
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
 * Instrumented test suite for verifying Firestore logging behavior inside ActivityLogger.
 *
 * This class ensures that:
 *  Logging an event creation inserts a new Firestore document with correct fields.
 *  Logging a profile removal produces a valid Firestore entry.
 *
 * Each test performs a Firestore write using ActivityLogger and then queries Firestore
 * with Tasks.await() to synchronously verify the result.
 */
/*
 * Citations:
 *
 * 1. Stack Overflow – "Make synchronous call to Firestore in Android device"
 *    Discusses how to use Tasks.await() to block until Firestore queries complete.
 *    https://stackoverflow.com/questions/55485758/make-synchronous-call-to-firestore-in-android-device
 *
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

    /**
     * Verifies that calling ActivityLogger.logEventCreated(...)
     * writes a Firestore log document with:
     *  type = "event_created"
     *  eventTitle equal to the test value
     *  a message that includes the event title
     *
     * @throws Exception if Firestore query fails or times out
     */
    @Test
    public void testLogEventCreatedWritesToFirestore() throws Exception {

        String testEventTitle = "Test Event " + System.currentTimeMillis();
        String testEventId = "event_" + System.currentTimeMillis();

        ActivityLogger.logEventCreated(testEventId, testEventTitle);

        Thread.sleep(2000);

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

    /**
     * Verifies that calling ActivityLogger.logProfileRemoved(...)
     * writes a Firestore log document with:
     * type = "profile_removed"
     * userEmail equal to the test value
     */
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

