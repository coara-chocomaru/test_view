package com.coara.view;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText searchQuery, startDate, endDate;
    private SharedPreferences preferences;
    private ArrayList<String> bookmarks = new ArrayList<>();
    
    private static final int MAX_QUERY_LENGTH = 40;
    private static final int MAX_DATE_LENGTH = 10;  // "YYYY/MM/DD"形式のため

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
        preferences = getSharedPreferences("Bookmarks", MODE_PRIVATE);

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

        // 入力制限の追加
        searchQuery.addTextChangedListener(new InputLimitWatcher(searchQuery, MAX_QUERY_LENGTH));
        addDateInputFormatting(startDate);
        addDateInputFormatting(endDate);

        loadBookmarks();
        searchButton.setOnClickListener(v -> performSearch());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
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
        } else if (itemId == R.id.action_bookmark) {
            addBookmark(webView.getUrl());
            return true;
        } else if (itemId == R.id.action_view_bookmarks) {
            showBookmarks();
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

                String input = editable.toString().replaceAll("[^0-9]", "");  // 数字以外を削除
                StringBuilder formatted = new StringBuilder();

                for (int i = 0; i < input.length(); i++) {
                    if (i == 4 || i == 6) formatted.append("/");
                    formatted.append(input.charAt(i));
                    if (formatted.length() == MAX_DATE_LENGTH) break;
                }

                // 入力後にカーソルが最後に来るように設定
                editText.setText(formatted);
                editText.setSelection(formatted.length());
                isFormatting = false;
            }
        });
    }

    private void addBookmark(String url) {
        if (url != null && !bookmarks.contains(url)) {
            bookmarks.add(url);
            saveBookmarks();
        }
    }

    private void saveBookmarks() {
        SharedPreferences.Editor editor = preferences.edit();
        JSONArray jsonArray = new JSONArray(bookmarks);
        editor.putString("bookmark_list", jsonArray.toString());
        editor.apply();
    }

    private void loadBookmarks() {
        String json = preferences.getString("bookmark_list", "[]");
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                bookmarks.add(jsonArray.getString(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showBookmarks() {
        StringBuilder bookmarkList = new StringBuilder();
        for (String bookmark : bookmarks) {
            bookmarkList.append(bookmark).append("\n");
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("ブックマーク一覧")
                .setMessage(bookmarkList.toString())
                .setPositiveButton("閉じる", null)
                .show();
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
                editable.delete(maxLength, editable.length());
            }
        }
    }
}
