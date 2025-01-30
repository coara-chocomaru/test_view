package com.coara.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText searchQuery, startDate, endDate;
    private boolean isInverted = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "ストレージの書き込み権限が必要です。", Toast.LENGTH_LONG).show();
                }
            });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        searchQuery = findViewById(R.id.searchQuery);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        Button searchButton = findViewById(R.id.searchButton);
        Button screenshotButton = findViewById(R.id.screenshotButton);
        Button invertButton = findViewById(R.id.invertButton);

        // WebView設定
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webView.evaluateJavascript(
                        "document.querySelector('[role=\"search\"]').style.display='none';", null);
            }
        });

        // 検索ボタン
        searchButton.setOnClickListener(v -> performSearch());

        // スクリーンショットボタン
        screenshotButton.setOnClickListener(v -> takeScreenshot());

        // ネガポジ反転ボタン
        invertButton.setOnClickListener(v -> toggleInvert());

        // 入力制限
        searchQuery.addTextChangedListener(new InputLimitWatcher(searchQuery, 40));
        addDateInputFormatting(startDate);
        addDateInputFormatting(endDate);

        // ストレージ権限の確認・リクエスト
        checkStoragePermission();

        // ボトムナビゲーション
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Android 10未満は権限が必要
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void takeScreenshot() {
        // WebViewをビットマップ化
        Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        webView.draw(canvas);

        // 画像を保存
        try {
            String fileName = "screenshot_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Screenshots");

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    Toast.makeText(this, "スクリーンショットを保存しました", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "スクリーンショットの保存に失敗しました", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleInvert() {
        isInverted = !isInverted;
        webView.evaluateJavascript("document.body.style.filter = '" + (isInverted ? "invert(1)" : "none") + "';", null);
    }

    private void performSearch() {
        try {
            String query = URLEncoder.encode(searchQuery.getText().toString().trim(), "UTF-8");
            String after = startDate.getText().toString().trim();
            String before = endDate.getText().toString().trim();

            StringBuilder searchUrl = new StringBuilder("https://www.google.com/search?q=");
            searchUrl.append(query);

            if (after.matches("\\d{4}/\\d{2}/\\d{2}")) searchUrl.append("+after:").append(after.replace("/", ""));
            if (before.matches("\\d{4}/\\d{2}/\\d{2}")) searchUrl.append("+before:").append(before.replace("/", ""));

            webView.loadUrl(searchUrl.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "検索エラー", Toast.LENGTH_LONG).show();
        }
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_back && webView.canGoBack()) {
            webView.goBack();
            return true;
        } else if (item.getItemId() == R.id.action_forward && webView.canGoForward()) {
            webView.goForward();
            return true;
        }
        return false;
    }

    private void addDateInputFormatting(EditText editText) {
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        editText.addTextChangedListener(new InputLimitWatcher(editText, 10));
    }

    private static class InputLimitWatcher implements TextWatcher {
        private final EditText editText;
        private final int maxLength;

        InputLimitWatcher(EditText editText, int maxLength) {
            this.editText = editText;
            this.maxLength = maxLength;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.length() > maxLength) editable.delete(maxLength, editable.length());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
