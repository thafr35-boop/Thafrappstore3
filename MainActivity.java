package com.abdulrahmandhafir.smartphones;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import org.json.JSONArray;

public class MainActivity extends Activity {
    private WebView webView;
    private ArrayList<String> pendingSharedBase64Images = new ArrayList<>();
    private boolean pageLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                flushPendingImagesToWebApp();
            }
        });

        handleGalleryShareIntent(getIntent());
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleGalleryShareIntent(intent);
        if (pageLoaded) {
            flushPendingImagesToWebApp();
        }
    }

    private void handleGalleryShareIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();

        if (type != null && type.startsWith("image/")) {
            if (Intent.ACTION_SEND.equals(action)) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    String b64 = uriToDataUrl(imageUri, type);
                    if (b64 != null) pendingSharedBase64Images.add(b64);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageUris != null) {
                    for (Uri uri : imageUris) {
                        String b64 = uriToDataUrl(uri, type);
                        if (b64 != null) pendingSharedBase64Images.add(b64);
                    }
                }
            }
        }
    }

    private String uriToDataUrl(Uri uri, String mimeType) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            is.close();
            String base64 = Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP);
            return "data:" + (mimeType != null ? mimeType : "image/jpeg") + ";base64," + base64;
        } catch (Exception e) {
            return null;
        }
    }

    private void flushPendingImagesToWebApp() {
        if (pendingSharedBase64Images.isEmpty()) return;
        JSONArray jsonArray = new JSONArray(pendingSharedBase64Images);
        pendingSharedBase64Images.clear();
        final String jsCode = "if (window.receiveSharedGalleryImages) { window.receiveSharedGalleryImages(" + jsonArray.toString() + "); }";
        webView.post(() -> webView.evaluateJavascript(jsCode, null));
    }
}
