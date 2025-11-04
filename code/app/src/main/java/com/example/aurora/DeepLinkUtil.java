package com.example.aurora;

import android.content.Intent;
import android.net.Uri;

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
