package com.coara.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText searchQuery, startDate, endDate;
    private static final int MAX_QUERY_LENGTH = 40;
    private static final int MAX_DATE_LENGTH = 10; 
    private boolean isInverted = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        searchQuery = findViewById(R.id.searchQuery);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        Button screenshotButton = findViewById(R.id.screenshotButton);
        Button invertButton = findViewById(R.id.invertButton);
        Button searchButton = findViewById(R.id.searchButton);

        
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setBackgroundColor(0xFFFFFF); 
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

        
        webView.loadUrl("about:blank");

        
        searchButton.setOnClickListener(v -> performSearch());

        
        screenshotButton.setOnClickListener(v -> takeScreenshot());

    
        invertButton.setOnClickListener(v -> toggleInvert());


        searchQuery.addTextChangedListener(new InputLimitWatcher(searchQuery, MAX_QUERY_LENGTH));
        addDateInputFormatting(startDate);
        addDateInputFormatting(endDate);

        
        checkStoragePermission();

        
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
    }

    @Override
    public void onBackPressed() {

    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
        
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void takeScreenshot() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            
            View rootView = getWindow().getDecorView().getRootView();
            rootView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
            rootView.setDrawingCacheEnabled(false);

        
            File directory = new File(Environment.getExternalStorageDirectory(), "DCIM");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";
            File file = new File(directory, fileName);

        
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                Toast.makeText(this, "スクリーンショットが保存されました: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "スクリーンショットの保存に失敗しました。", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "ストレージ権限が必要です。", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleInvert() {
        if (isInverted) {

            webView.evaluateJavascript("document.body.style.filter = 'none';", null);
        } else {
        
            webView.evaluateJavascript("document.body.style.filter = 'invert(1)';", null);
        }
        isInverted = !isInverted;
    }

    private void performSearch() {
        String query = searchQuery.getText().toString().trim();
        String after = startDate.getText().toString().trim();
        String before = endDate.getText().toString().trim();

        
        StringBuilder searchUrl = new StringBuilder("https://www.google.com/search?q=");
        searchUrl.append(query.replace(" ", "+"));

        if (after.matches("\\d{4}/\\d{2}/\\d{2}")) searchUrl.append("+after:").append(after);
        if (before.matches("\\d{4}/\\d{2}/\\d{2}")) searchUrl.append("+before:").append(before);

        webView.loadUrl(searchUrl.toString());
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_back) {
            if (webView.canGoBack()) webView.goBack();
            return true;
        } else if (itemId == R.id.action_forward) {
            if (webView.canGoForward()) webView.goForward();
            return true;
        } else {
            return false;
        }
    }

    private void addDateInputFormatting(EditText editText) {
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_NORMAL);
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormatting) return;
                isFormatting = true;

                String input = editable.toString().replaceAll("[^0-9]", "");  
                StringBuilder formatted = new StringBuilder();

                for (int i = 0; i < input.length(); i++) {
                    if (i == 4 || i == 6) formatted.append("/");
                    formatted.append(input.charAt(i));
                    if (formatted.length() == MAX_DATE_LENGTH) break;
                }

            
                editText.setText(formatted);
                editText.setSelection(formatted.length());
                isFormatting = false;
            }
        });
    }

    private static class InputLimitWatcher implements TextWatcher {
        private final EditText editText;
        private final int maxLength;

        InputLimitWatcher(EditText editText, int maxLength) {
            this.editText = editText;
            this.maxLength = maxLength;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.length() > maxLength) {
                editText.setText(editable.subSequence(0, maxLength));
                editText.setSelection(maxLength);
            }
        }
    }
}
