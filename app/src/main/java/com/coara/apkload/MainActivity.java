package com.coara.apkload;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_APK_PICK = 1;
    private File unpackDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        unpackDir = new File(getFilesDir(), "unpack");
        if (!unpackDir.exists()) unpackDir.mkdirs();

        findViewById(R.id.btn_pick_apk).setOnClickListener(v -> selectApkFile());
        findViewById(R.id.btn_show_folders).setOnClickListener(v -> showFolders());
    }

    private void selectApkFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_APK_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_APK_PICK && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) extractApk(uri);
        }
    }

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
            Toast.makeText(this, "解凍エラー", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

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

    private void startDexActivity(File apkFolder) {
        Intent intent = new Intent(this, ProxyActivity.class);
        intent.putExtra("apkPath", apkFolder.getAbsolutePath());
        startActivity(intent);
    }
}
