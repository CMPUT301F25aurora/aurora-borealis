/*
 * References:
 *
 * 1) Firebase — "Get started with Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/quickstart
 *    Used as a reference for initializing FirebaseFirestore and reading user documents for login.
 *
 * 2) author: Stack Overflow user — "Firebase Firestore get data from collection"
 *    https://stackoverflow.com/questions/46706433/firebase-firestore-get-data-from-collection
 *    Used as a reference for looking up user records in Firestore using email or phone as a key.
 *
 * 3) Android Developers — "Settings.Secure"
 *    https://developer.android.com/reference/android/provider/Settings.Secure
 *    Used as a reference for using Settings.Secure.ANDROID_ID to create or find a default entrant profile by device.
 *
 * 4) Android Developers — "Data and file storage overview (SharedPreferences)"
 *    https://developer.android.com/training/data-storage/shared-preferences
 *    https://developer.android.com/topic/libraries/architecture/datastore
 *    https://developer.android.com/kotlin/multiplatform/datastore
 *    Used as a reference for saving the logged-in user's role, name, email, and phone in SharedPreferences.
 */


package com.example.aurora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles user login and navigation in the Aurora app.
 *
 * <p>This activity is responsible for:
 * - Checking if a user is already logged in and sending them to the right page.
 * - Creating a default entrant profile for new devices.
 * - Logging in users using their email or phone and password.
 * - Saving user information locally using SharedPreferences.
 * - Opening the correct page based on the user's role (Admin, Organizer, Entrant).</p>
 *
 * <p>Firestore collection: "users"</p>
 * <p>Shared preferences: "aurora_prefs" and "aurora"</p>
 */


public class LoginActivity extends BaseActivity {

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private TextView createAccountButton;
    private FirebaseFirestore db;
    /**
     * Initializes the login screen, checks for previously stored login data,
     * and redirects returning users automatically.
     *
     * <p>If the user is not already logged in, this method loads the login layout,
     * sets up UI listeners (password toggle, login button, create-account button),
     * initializes Firestore, and generates a default entrant profile for the device
     * if one does not already exist.</p>
     *
     * @param savedInstanceState Previously saved UI state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        String role = sp.getString("user_role", null);
        String name = sp.getString("user_name", null);
        String email = sp.getString("user_email", null);
        String phone = sp.getString("user_phone", null);

        if (role != null && !role.isEmpty()) {
            Intent intent;
            if (role.equalsIgnoreCase("admin")) {
                intent = new Intent(this, AdminActivity.class);
            } else if (role.equalsIgnoreCase("organizer")) {
                intent = new Intent(this, OrganizerActivity.class);
            } else {
                intent = new Intent(this, EventsActivity.class);
            }

            intent.putExtra("userName", name);
            intent.putExtra("userEmail", email);
            intent.putExtra("userPhone", phone);
            intent.putExtra("userRole", role);

            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);
        db = FirebaseFirestore.getInstance();

        ImageView passwordToggle = findViewById(R.id.passwordToggle);

        final boolean[] isVisible = {false};

        passwordToggle.setOnClickListener(v -> {
            if (isVisible[0]) {
                // Hide password
                loginPassword.setTransformationMethod(new PasswordTransformationMethod());
                passwordToggle.setImageResource(R.drawable.ic_eye_closed);
            } else {
                // Show password
                loginPassword.setTransformationMethod(null);
                passwordToggle.setImageResource(R.drawable.ic_eye_open);
            }

            isVisible[0] = !isVisible[0];
            loginPassword.setSelection(loginPassword.getText().length());
        });


        createDefaultEntrantProfile();

        loginButton.setOnClickListener(v -> loginUser());
        createAccountButton.setOnClickListener(v -> {
            Intent i = new Intent(this, SignUpActivity.class);

            // pass pending event if exists
            String pending = getSharedPreferences("aurora", MODE_PRIVATE)
                    .getString("pending_event", null);
            if (pending != null) {
                i.putExtra("pending_event", pending);
            }

            startActivity(i);
        });
    }

    /**
     * Creates a default entrant profile tied to the unique Android device ID.
     *
     * <p>This allows users to participate in events even without creating a full
     * account. If a Firestore document already exists for this device, nothing is changed.</p>
     *
     * <p>Stored fields include:</p>
     * <ul>
     *     <li>role = "entrant"</li>
     *     <li>notificationsEnabled = false</li>
     *     <li>createdAt timestamp</li>
     *     <li>deviceId (ANDROID_ID)</li>
     * </ul>
     *
     * Logs success or failure using Logcat.
     */
    private void createDefaultEntrantProfile() {

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        DocumentReference userRef = db.collection("users").document(deviceId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String, Object> user = new HashMap<>();
                user.put("role", "entrant");
                user.put("notificationsEnabled", false);
                user.put("createdAt", System.currentTimeMillis());
                user.put("deviceId", deviceId);

                userRef.set(user)
                        .addOnSuccessListener(aVoid ->
                                Log.d("DeviceAuth", "✅ New entrant profile created for device: " + deviceId))
                        .addOnFailureListener(e ->
                                Log.e("DeviceAuth", "❌ Error creating entrant profile", e));
            } else {
                Log.d("DeviceAuth", "Existing entrant profile found for device: " + deviceId);
            }
        }).addOnFailureListener(e ->
                Log.e("DeviceAuth", "❌ Failed to check entrant profile", e));
    }

    /**
     * Attempts to authenticate the user by checking their input (email or phone)
     * along with their password against Firestore records.
     *
     * <p>Login order:</p>
     * <ol>
     *     <li>Try matching input as an email.</li>
     *     <li>If email lookup fails, try matching input as a phone number.</li>
     * </ol>
     *
     * <p>If a valid user document is found, {@link #handleLogin(DocumentSnapshot)}
     * is called to complete the login process. Otherwise, an error toast is shown.</p>
     */
    private void loginUser() {
        String input = loginEmail.getText().toString().trim();    // email or phone
        String password = loginPassword.getText().toString().trim();

        if (input.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter both email/phone and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) try email
        db.collection("users")
                .whereEqualTo("email", input)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        handleLogin(query.getDocuments().get(0));
                    } else {
                        // 2) fallback phone
                        db.collection("users")
                                .whereEqualTo("phone", input)
                                .whereEqualTo("password", password)
                                .get()
                                .addOnSuccessListener(phoneQuery -> {
                                    if (!phoneQuery.isEmpty()) {
                                        handleLogin(phoneQuery.getDocuments().get(0));
                                    } else {
                                        Toast.makeText(this,
                                                "Invalid email/phone or password",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    /**
     * Completes the login process after a matching Firestore document is found.
     *
     * <p>This method:</p>
     * <ul>
     *     <li>Extracts user fields from Firestore (name, email, phone, role)</li>
     *     <li>Saves the data in SharedPreferences for future auto-login</li>
     *     <li>Handles deep-link navigation for pending event scans</li>
     *     <li>Opens the correct activity based on role:
     *         <ul>
     *             <li>Admin → AdminActivity</li>
     *             <li>Organizer → OrganizerActivity</li>
     *             <li>Entrant → EventsActivity</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param doc The Firestore user document representing the logged-in user.
     */
    private void handleLogin(DocumentSnapshot doc) {

        String name = doc.getString("name");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String role  = doc.getString("role");

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        sp.edit()
                .putString("user_email", email == null ? "" : email)
                .putString("user_name",  name  == null ? "" : name)
                .putString("user_role",  role  == null ? "" : role)
                .putString("user_doc_id", doc.getId())
                .apply();

        Toast.makeText(this, "Welcome " + (name == null ? "" : name), Toast.LENGTH_SHORT).show();

        // Deep-link case (scanned QR before login)
        String pending = getSharedPreferences("aurora", MODE_PRIVATE)
                .getString("pending_event", null);
        if (pending != null) {
            getSharedPreferences("aurora", MODE_PRIVATE)
                    .edit()
                    .remove("pending_event")
                    .apply();

            Intent deepLinkIntent = new Intent(this, EventDetailsActivity.class);
            deepLinkIntent.putExtra("eventId", pending);
            startActivity(deepLinkIntent);
            finish();
            return;
        }

        Intent intent;
        if (role != null && role.equalsIgnoreCase("admin")) {
            intent = new Intent(this, AdminActivity.class);
        } else if (role != null && role.equalsIgnoreCase("organizer")) {
            intent = new Intent(this, OrganizerActivity.class);
        } else {
            intent = new Intent(this, EventsActivity.class);
        }

        intent.putExtra("userName",  name);
        intent.putExtra("userEmail", email);
        intent.putExtra("userPhone", phone);
        intent.putExtra("userRole",  role);

        startActivity(intent);
        finish();
    }
}
