package com.example.aurora;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Displays the alerts screen for entrants in the Aurora app.
 *
 * <p>This activity shows important notifications or alerts that may
 * have been sent by organizers or the system. It includes a simple
 * back button for navigation and uses the <code>fragment_alerts</code>
 * layout to display its UI content.</p>
 *
 * <p>The screen is lightweight and mainly serves as a placeholder
 * for displaying in-app alerts or messages in future versions.</p>
 */

/*
 * Sources / citations for AlertsActivity.
 *
 * source: Android docs - "Add buttons to your app".
 * url: https://developer.android.com/develop/ui/views/components/button
 * note: Used as a reference for attaching a click listener with setOnClickListener
 *       to handle UI button taps.
 *
 * source: Android ImageButton example (tutorial).
 * url: https://mkyong.com/android/android-imagebutton-example/
 * note: Used for the basic pattern of defining an ImageButton in XML and wiring it
 *       up in an activity with findViewById.
 *
 * source: Stack Overflow user - "How set the onClick event in a ImageButton?".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/29070625/how-set-the-onclick-event-in-a-imagebutton
 * note: General reference for handling ImageButton clicks with an OnClickListener.
 *
 * source: Stack Overflow user - "how to override action bar back button in android?".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/14437745/how-to-override-action-bar-back-button-in-android
 * note: Used as a reference for calling onBackPressed() from a UI element to navigate back.
 *
 * source: ChatGPT (OpenAI assistant).
 * note: Helped choose the simple pattern of wiring the alerts back ImageButton to
 *       onBackPressed(), without changing any platform behavior.
 */

public class AlertsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_alerts);

        ImageButton back = findViewById(R.id.backButtonAlerts);
        if (back != null) {
            back.setOnClickListener(v -> onBackPressed());
        }
    }
}
