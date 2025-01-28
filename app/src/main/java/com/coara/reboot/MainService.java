package com.coara.reboot;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainService extends Service {
    private static final String TAG = "RebootService";
    private static final String MTK_SU = "mtk-su";
    private static final String CACHE_DIR = "/data/data/com.coara.reboot/cache/";
    private static final String ID_COMMAND = CACHE_DIR + MTK_SU + " -c id";
    private static final String REBOOT_COMMAND = CACHE_DIR + MTK_SU + " -c reboot";
    private static final String APK_PATH = "/data/app/com.coara.reboot-1/base.apk";
    private static final String MOUNT_COMMAND = CACHE_DIR + MTK_SU + " -c mount --bind " + APK_PATH + " /system/priv-app/Contacts/Contacts.apk";
    private static final String KILL_ZYGOTE_COMMAND = CACHE_DIR + MTK_SU + " -c killall zygote";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        new Thread(() -> {
            try {
                // キャッシュディレクトリを確認・作成
                ensureCacheDirectory();

                // mtk-su をキャッシュディレクトリにコピー
                copyAssetToCache(MTK_SU);

                // id コマンドで現在のコンテキストを確認
                if (isPrivAppContext()) {
                    Log.d(TAG, "Priv-app context detected, performing reboot.");
                    executeCommand(REBOOT_COMMAND);
                } else {
                    Log.d(TAG, "Priv-app context not detected, proceeding with fallback operations.");
                    executeFallback();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in service operation", e);
            } finally {
                stopSelf(); // 処理終了後にサービスを停止
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // バインド不要のため null を返す
    }

    private void ensureCacheDirectory() {
        File cacheDir = new File(CACHE_DIR);
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Log.e(TAG, "Failed to create cache directory.");
            throw new RuntimeException("Cache directory creation failed.");
        }
        Log.d(TAG, "Cache directory is ready: " + CACHE_DIR);
    }

    private void copyAssetToCache(String fileName) throws Exception {
        File outFile = new File(CACHE_DIR + fileName);
        if (!outFile.exists()) {
            try (InputStream in = getAssets().open(fileName);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            if (!outFile.setExecutable(true, false)) {
                throw new RuntimeException("Failed to set executable permission for: " + outFile.getAbsolutePath());
            }
            Log.d(TAG, "Copied " + fileName + " to cache with executable permission.");
        } else {
            Log.d(TAG, fileName + " already exists in cache.");
        }
    }

    private boolean executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            return process.waitFor() == 0;
        } catch (Exception e) {
            Log.e(TAG, "Command execution failed: " + command, e);
            return false;
        }
    }

    private boolean isPrivAppContext() {
        try {
            Process process = Runtime.getRuntime().exec(ID_COMMAND);
            InputStream inputStream = process.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            String output = new String(buffer, 0, bytesRead);
            Log.d(TAG, "id command output: " + output);
            return output.contains("priv_app");
        } catch (Exception e) {
            Log.e(TAG, "Error executing id command", e);
            return false;
        }
    }

    private void executeFallback() {
        try {
            // Mount コマンドを実行
            if (executeCommand(MOUNT_COMMAND)) {
                Log.d(TAG, "Mount command executed successfully.");
            } else {
                Log.e(TAG, "Mount command failed.");
            }

            // Zygote プロセスを強制終了
            if (executeCommand(KILL_ZYGOTE_COMMAND)) {
                Log.d(TAG, "Zygote process killed successfully.");
            } else {
                Log.e(TAG, "Failed to kill zygote process.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing fallback operations", e);
        }
    }
}
