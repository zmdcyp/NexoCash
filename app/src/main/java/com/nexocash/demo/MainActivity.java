package com.nexocash.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraPhotoUri;
    private static final int FILE_CHOOSER_REQUEST = 1;
    private static final int CAMERA_PERMISSION_REQUEST = 2;

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
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                } else {
                    launchChooser();
                }
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void launchChooser() {
        List<Intent> cameraIntents = new ArrayList<>();
        cameraPhotoUri = null;

        // Always try to create camera intent - don't use resolveActivity on Android 11+
        try {
            File photoFile = createImageFile();
            cameraPhotoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraIntents.add(takePictureIntent);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Gallery intent
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryIntent.setType("image/*");

        // Build chooser
        Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Photo");
        if (!cameraIntents.isEmpty()) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                    cameraIntents.toArray(new Intent[0]));
        }

        try {
            startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
                filePathCallback = null;
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("NEXO_" + timeStamp + "_", ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            launchChooser();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getDataString() != null) {
                        // Gallery pick
                        results = new Uri[]{Uri.parse(data.getDataString())};
                    } else if (cameraPhotoUri != null) {
                        // Camera capture
                        results = new Uri[]{cameraPhotoUri};
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
