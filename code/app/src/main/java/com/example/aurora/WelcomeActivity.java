package com.example.aurora;

/**
 * WelcomeActivity serves as the app's splash or intro screen.
 * It is typically the first screen shown when the app launches
 * Displays a simple “tap anywhere” prompt to continue.
 * When the user taps anywhere on the screen, it navigates to LoginActivity.
 */

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private View root;
    private View tapAnywhere;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        root = findViewById(R.id.welcomeRoot);
        tapAnywhere = findViewById(R.id.tapAnywhere);

        View.OnClickListener goToLogin = v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
        };

        root.setOnClickListener(goToLogin);
        tapAnywhere.setOnClickListener(goToLogin);
    }
}
