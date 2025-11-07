/*
 * References:
 *
 * 1) author: Stack Overflow user — "How to implement my very own URI scheme on Android"
 *    https://stackoverflow.com/questions/2448213/how-to-implement-my-very-own-uri-scheme-on-android
 *    Used as a reference for defining a custom URI scheme and reading scheme/host/path from Intent.getData().
 *
 * 2) Android Developers — "Create deep links"
 *    https://developer.android.com/training/app-links/create-deeplinks
 *    Used as a reference for how deep links pass IDs in the path or query parameters and how activities receive them.
 */

package com.example.aurora;

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