/*
 * References for this screen:
 *
 * 1) source: Firebase docs — "Authenticate with Firebase using Password-Based Accounts on Android"
 *    https://firebase.google.com/docs/auth/android/password-auth
 *    Used for createUserWithEmailAndPassword and handling sign-up callbacks.
 *
 * 2) author: Stack Overflow user — "How to include username when storing email and password using Firebase"
 *    https://stackoverflow.com/questions/32151178/how-do-you-include-a-username-when-storing-email-and-password-using-firebase-ba
 *    Used for storing extra profile fields in Firestore right after auth sign-up.
 *
 * 3) source: Android Developers — "Understand the Activity lifecycle"
 *    https://developer.android.com/guide/components/activities/activity-lifecycle
 *    Used for managing navigation after a successful sign up and finishing this Activity.
 */


package com.example.aurora;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * SignUpActivity
 *
 * Handles the registration flow for new users in the Aurora Chance app.
 * This activity:
 * <ul>
 *     <li>Validates all required user input</li>
 *     <li>Creates a Firebase Authentication account (email + password)</li>
 *     <li>Writes additional user profile data to Firestore</li>
 *     <li>Prevents duplicate email accounts via FirebaseAuth</li>
 *     <li>Stores user information in SharedPreferences</li>
 *     <li>Redirects users to the correct dashboard based on their role
 *         (OrganizerActivity or EventsActivity)</li>
 *     <li>Logs registration events to the Firestore "logs" collection</li>
 * </ul>
 *
 * Supported roles:
 * <ul>
 *     <li>Entrant</li>
 *     <li>Organizer</li>
 * </ul>
 *
 * This class relies on:
 * <ul>
 *     <li>Firebase Authentication</li>
 *     <li>Firebase Firestore</li>
 *     <li>Android SharedPreferences</li>
 * </ul>
 */
public class SignUpActivity extends BaseActivity {

    private EditText signupName, signupEmail, signupPhone, signupPassword;
    private RadioGroup radioGroupRole;
    private Button signupButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        signupName = findViewById(R.id.Name);
        signupEmail = findViewById(R.id.Email);
        signupPhone = findViewById(R.id.Phone);
        signupPassword = findViewById(R.id.Password);
        radioGroupRole = findViewById(R.id.Role);
        signupButton = findViewById(R.id.SignUpButton);

        setupPasswordToggle();
        signupButton.setOnClickListener(v -> attemptSignup());

        findViewById(R.id.backToLoginButton).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }
    /**
     * Sets up the password visibility toggle for the password EditText.
     * Allows users to show/hide their password by tapping the eye icon.
     */
    private void setupPasswordToggle() {
        final boolean[] visible = {false};
        ImageView toggle = findViewById(R.id.passwordToggleSignup);

        toggle.setOnClickListener(v -> {
            if (visible[0]) {
                signupPassword.setTransformationMethod(new PasswordTransformationMethod());
                toggle.setImageResource(R.drawable.ic_eye_closed);
            } else {
                signupPassword.setTransformationMethod(null);
                toggle.setImageResource(R.drawable.ic_eye_open);
            }
            visible[0] = !visible[0];
            signupPassword.setSelection(signupPassword.getText().length());
        });
    }

    /**
     * Validates all user input fields before attempting to create an account.
     * Ensures that:
     * <ul>
     *     <li>All fields are filled</li>
     *     <li>Phone number is exactly 10 digits</li>
     *     <li>A role is selected (entrant or organizer)</li>
     * </ul>
     *
     * If validation passes, the Firebase Authentication account is created.
     */
    private void attemptSignup() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String phone = signupPhone.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();
        int selectedId = radioGroupRole.getCheckedRadioButtonId();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            toast("Please fill in all fields");
            return;
        }

        if (!phone.matches("\\d{10}")) {
            toast("Phone number must be exactly 10 digits");
            return;
        }

        String role = (selectedId == R.id.Organizer) ? "organizer" : "entrant";
        createAuthAccount(name, email, phone, password, role);
    }

    /**
     * Creates a new Firebase Authentication account using email + password.
     * If successful, continues by writing the user's profile to Firestore.
     *
     * FirebaseAuth automatically prevents duplicate email accounts.
     *
     * @param name     The user's full name.
     * @param email    The user's email address.
     * @param phone    The user's phone number (10 digits).
     * @param password The user's chosen password.
     * @param role     The selected user role ("entrant" or "organizer").
     */
    private void createAuthAccount(String name, String email, String phone,
                                   String password, String role) {


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        handleAuthFailure(task.getException());
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        toast("Could not create account.");
                        return;
                    }

                    writeUserToFirestore(firebaseUser.getUid(), name, email, phone, password, role);
                });
    }

    /**
     * Handles authentication errors from Firebase.
     * Distinguishes between:
     * <ul>
     *     <li>Email already in use</li>
     *     <li>General authentication errors</li>
     * </ul>
     *
     * @param e The exception thrown by FirebaseAuth.
     */
    private void handleAuthFailure(Exception e) {

        if (e instanceof FirebaseAuthUserCollisionException) {
            toast("An account with this email already exists.");
        } else if (e != null) {
            toast("Error: " + e.getMessage());
        } else {
            toast("Sign up failed.");
        }
    }

    /**
     * Writes the created user's profile to Firestore under the "users" collection.
     * Fields stored:
     * <ul>
     *     <li>name</li>
     *     <li>email</li>
     *     <li>phone</li>
     *     <li>role</li>
     *     <li>password (stored as provided)</li>
     * </ul>
     *
     * On success:
     * <ul>
     *     <li>Shows a success message</li>
     *     <li>Logs registration to Firestore "logs"</li>
     *     <li>Saves user profile locally in SharedPreferences</li>
     *     <li>Navigates to the correct home screen (organizer or entrant)</li>
     * </ul>
     *
     * @param uid      The Firebase Authentication user ID.
     * @param name     User's name.
     * @param email    User's email.
     * @param phone    User's phone number.
     * @param password User's password.
     * @param role     User's chosen role.
     */
    private void writeUserToFirestore(String uid, String name, String email, String phone,
                                      String password, String role) {


        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", role);
        user.put("password", password);

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(v -> {
                    toast("Account created!");
                    logRegistration(uid, email, role);
                    savePreferences(name, email, role, uid);
                    navigateAfterSignup(role, name, email, phone);
                })
                .addOnFailureListener(e ->
                        toast("Error: " + e.getMessage()));
    }

    /**
     * Logs the registration of a new user to the Firestore "logs" collection.
     * Includes:
     * <ul>
     *     <li>Log type</li>
     *     <li>Message</li>
     *     <li>Timestamp</li>
     *     <li>User ID</li>
     *     <li>User email</li>
     *     <li>User role</li>
     * </ul>
     *
     * @param uid   The Firebase user ID.
     * @param email The registered email.
     * @param role  The user's role (entrant or organizer).
     */
    private void logRegistration(String uid, String email, String role) {
        Map<String, Object> log = new HashMap<>();
        log.put("type", "user_registered");
        log.put("message", "User registered: " + email);
        log.put("timestamp", FieldValue.serverTimestamp());
        log.put("userId", uid);
        log.put("userEmail", email);
        log.put("userRole", role);

        db.collection("logs").add(log);
    }

    /**
     * Saves user profile data locally using SharedPreferences.
     * This allows the app to persist user session data across launches.
     *
     * @param name User's name.
     * @param email User's email.
     * @param role User's role.
     * @param uid Firebase UID.
     */
    private void savePreferences(String name, String email, String role, String uid) {

        getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .putString("user_role", role)
                .putString("user_doc_id", uid)
                .apply();
    }

    /**
     * Navigates the newly registered user to the correct home screen
     * based on their selected role.
     * <ul>
     *     <li>Organizer → OrganizerActivity</li>
     *     <li>Entrant → EventsActivity</li>
     * </ul>
     *
     * Passes user profile fields via Intent extras.
     *
     * @param role  User role.
     * @param name  User name.
     * @param email User email.
     * @param phone User phone.
     */
    private void navigateAfterSignup(String role, String name, String email, String phone) {

        Intent intent = role.equals("organizer")
                ? new Intent(this, OrganizerActivity.class)
                : new Intent(this, EventsActivity.class);

        intent.putExtra("userName", name);
        intent.putExtra("userEmail", email);
        intent.putExtra("userPhone", phone);
        intent.putExtra("userRole", role);

        startActivity(intent);
        finish();
    }

    /**
     * Helper method to show short Toast messages.
     *
     * @param msg The message to display.
     */
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
