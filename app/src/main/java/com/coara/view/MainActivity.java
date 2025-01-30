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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // WebView, EditText, Button, SharedPreferences の初期化
        webView = findViewById(R.id.webView);
        searchQuery = findViewById(R.id.searchQuery);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        Button searchButton = findViewById(R.id.searchButton);
        preferences = getSharedPreferences("Bookmarks", MODE_PRIVATE);

        // WebView 設定
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
                // Google検索結果の検索バーを非表示にするCSSを適用
                webView.evaluateJavascript(
                        "document.querySelector('[role=\"search\"]').style.display='none';", null);
            }
        });

        // 日時入力の自動修正
        addDateInputFormatting(startDate);
        addDateInputFormatting(endDate);

        // ブックマークを読み込む
        loadBookmarks();

        // 検索ボタンのクリックリスナー設定
        searchButton.setOnClickListener(v -> performSearch());

        // BottomNavigationView の設定
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
    }

    // 検索処理
    private void performSearch() {
        String query = searchQuery.getText().toString().trim();
        String after = startDate.getText().toString().trim();
        String before = endDate.getText().toString().trim();

        StringBuilder searchUrl = new StringBuilder("https://www.google.com/search?q=");
        searchUrl.append(query.replace(" ", "+"));

        if (after.matches("\\d{4}/\\d{2}/\\d{2}")) searchUrl.append("+after:").append(after);
        if (before.matches("\\d{4}/\\d{2}/\\d{2}")) searchUrl.append("+before:").append(before);

        // WebView に URL を読み込む
        webView.loadUrl(searchUrl.toString());
    }

    // BottomNavigation のアイテム選択時の処理
    private boolean onNavItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_back) {
            if (webView.canGoBack()) webView.goBack();
            return true;
        } else if (item.getItemId() == R.id.action_forward) {
            if (webView.canGoForward()) webView.goForward();
            return true;
        } else if (item.getItemId() == R.id.action_bookmark) {
            addBookmark(webView.getUrl());
            return true;
        } else if (item.getItemId() == R.id.action_view_bookmarks) {
            showBookmarks();
            return true;
        } else {
            return false;
        }
    }

    // 日時入力欄の自動修正 (YYYY/MM/DD 形式)
    private void addDateInputFormatting(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String input = editable.toString();

                if (input.length() == 4 && !input.contains("/")) {
                    editable.append("/");
                } else if (input.length() == 7 && !input.contains("/")) {
                    editable.append("/");
                }

                // 最大文字数制限
                if (input.length() > 10) {
                    editable.delete(10, editable.length());
                }

                // YYYY/MM/DD 形式以外の入力を制限
                if (!input.matches("\\d{0,4}/?\\d{0,2}/?\\d{0,2}")) {
                    editable.delete(editable.length() - 1, editable.length());
                }
            }
        });
    }

    // ブックマーク追加処理
    private void addBookmark(String url) {
        if (url != null && !bookmarks.contains(url)) {
            bookmarks.add(url);
            saveBookmarks();
        }
    }

    // ブックマークの保存
    private void saveBookmarks() {
        SharedPreferences.Editor editor = preferences.edit();
        JSONArray jsonArray = new JSONArray(bookmarks);
        editor.putString("bookmark_list", jsonArray.toString());
        editor.apply();
    }

    // ブックマークの読み込み
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

    // ブックマークの表示
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
}
