package com.coara.apkload;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {
    private File unpackDir;
    private ActivityResultLauncher<Intent> apkPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        unpackDir = new File(getFilesDir(), "unpack");
        if (!unpackDir.exists()) unpackDir.mkdirs();

        // 権限確認
        checkStoragePermission();

        // APK選択用 ActivityResultLauncher
        apkPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) extractApk(uri);
                    }
                });

        findViewById(R.id.btn_pick_apk).setOnClickListener(v -> selectApkFile());
        findViewById(R.id.btn_show_folders).setOnClickListener(v -> showFolders());
    }

    // ストレージアクセス権限の確認とリクエスト
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("権限が必要")
                        .setMessage("APKを読み込むためにストレージアクセス権限が必要です。")
                        .setPositiveButton("OK", (dialog, which) -> checkStoragePermission())
                        .setNegativeButton("キャンセル", null)
                        .show();
            }
        }
    }

    // APK選択ダイアログを開く
    private void selectApkFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        apkPickerLauncher.launch(intent);
    }

    // APKを解凍する
    private void extractApk(Uri apkUri) {
        try {
            String apkName = getFileName(apkUri);
            File apkDir = new File(unpackDir, apkName);
            if (!apkDir.exists()) apkDir.mkdirs();

            try (InputStream in = getContentResolver().openInputStream(apkUri);
                 ZipInputStream zipIn = new ZipInputStream(in)) {

                ZipEntry entry;
                while ((entry = zipIn.getNextEntry()) != null) {
                    File outFile = new File(apkDir, entry.getName());
                    if (!outFile.getCanonicalPath().startsWith(apkDir.getCanonicalPath())) {
                        // Zipスリップ攻撃防止
                        throw new SecurityException("不正なZIPエントリ: " + entry.getName());
                    }
                    if (entry.isDirectory()) outFile.mkdirs();
                    else {
                        try (FileOutputStream out = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = zipIn.read(buffer)) > 0) out.write(buffer, 0, length);
                        }
                    }
                    zipIn.closeEntry();
                }
                Toast.makeText(this, "解凍完了: " + apkName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "解凍エラー: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // APKファイル名を取得する
    private String getFileName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String fileName = cursor.getString(nameIndex);
            cursor.close();
            return fileName.replace(".apk", "");
        }
        return "unknown_apk";
    }

    // 解凍済みAPKのフォルダ一覧を表示
    private void showFolders() {
        LinearLayout layout = findViewById(R.id.folder_list);
        layout.removeAllViews();

        File[] folders = unpackDir.listFiles();
        if (folders != null) {
            for (File folder : folders) {
                if (folder.isDirectory()) {
                    Button btn = new Button(this);
                    btn.setText(folder.getName());
                    btn.setOnClickListener(v -> startDexActivity(folder));
                    layout.addView(btn);
                }
            }
        }
    }

    // ProxyActivityを使って外部APKのMainActivityを起動
    private void startDexActivity(File apkFolder) {
        Intent intent = new Intent(this, ProxyActivity.class);
        intent.putExtra("apkPath", apkFolder.getAbsolutePath());
        startActivity(intent);
    }
}
