/*
 * source: Android Developers — "Create Deep Links".
 * url: https://developer.android.com/training/app-links/deep-linking
 * note: Reference for understanding the structure of incoming Intents (scheme, host, path).
 *
 * source: Android Developers — "Uri".
 * url: https://developer.android.com/reference/android/net/Uri
 * note: Used for parsing query parameters (getQueryParameter) and path segments.
 */

package com.example.aurora.utils;

import android.content.Intent;
import android.net.Uri;
/**
 * aurora://event/&lt;id&gt; deep link format.
 */



public class DeepLinkUtil {
    public static String extractEventIdFromIntent(Intent intent) {
        if (intent == null) return null;
        Uri data = intent.getData();
        if (data == null) return null;
        if ("aurora".equalsIgnoreCase(data.getScheme())
                && "event".equalsIgnoreCase(data.getHost())) {
            if (data.getPath() != null && data.getPath().length() > 1)
                return data.getPath().substring(1); // /<id>
            String qId = data.getQueryParameter("id");
            if (qId != null && !qId.isEmpty()) return qId;
        }
        return null;
    }
}
