package com.example.aurora;

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
            // do not call finish() so back from Login returns here
        };

        root.setOnClickListener(goToLogin);
        tapAnywhere.setOnClickListener(goToLogin);
    }
}
