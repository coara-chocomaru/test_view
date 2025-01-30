package com.coara.view;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

        webView = findViewById(R.id.webView);
        searchQuery = findViewById(R.id.searchQuery);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        Button searchButton = findViewById(R.id.searchButton);
        preferences = getSharedPreferences("Bookmarks", MODE_PRIVATE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        
        loadBookmarks();

        searchButton.setOnClickListener(v -> performSearch());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnNavigationItemSelectedListener(this::onNavItemSelected);
    }

    private void performSearch() {
        String query = searchQuery.getText().toString().trim();
        String after = startDate.getText().toString().trim();
        String before = endDate.getText().toString().trim();
        
        StringBuilder searchUrl = new StringBuilder("https://www.google.com/search?q=");
        searchUrl.append(query.replace(" ", "+"));

        if (!after.isEmpty()) searchUrl.append("+after:").append(after);
        if (!before.isEmpty()) searchUrl.append("+before:").append(before);

        webView.loadUrl(searchUrl.toString());
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_back:
                if (webView.canGoBack()) webView.goBack();
                return true;
            case R.id.action_forward:
                if (webView.canGoForward()) webView.goForward();
                return true;
            case R.id.action_bookmark:
                addBookmark(webView.getUrl());
                return true;
            case R.id.action_view_bookmarks:
                showBookmarks();
                return true;
            default:
                return false;
        }
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
}
