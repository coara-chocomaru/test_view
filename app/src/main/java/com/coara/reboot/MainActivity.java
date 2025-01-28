package com.coara.reboot;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final String TAG = "RebootApp";
    private static final String MTK_SU = "mtk-su";
    private static final String APK_PATH = "/data/app/com.coara.reboot-1/base.apk";
    private static final String CACHE_DIR = "/data/data/com.coara.reboot/cache/";
    private static final String REBOOT_COMMAND = CACHE_DIR + MTK_SU + " -c reboot";
    private static final String MOUNT_COMMAND = CACHE_DIR + MTK_SU + " -c mount --bind " + APK_PATH + " /system/priv-app/Contacts/Contacts.apk";
    private static final String KILL_ZYGOTE_COMMAND = CACHE_DIR + MTK_SU + " -c killall zygote"; 
    private static final String CHECK_PROCESS_COMMAND = CACHE_DIR + MTK_SU + " -c ps | grep com.coara.reboot";
    private static final int REBOOT_TIMEOUT_MS = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 画面に「reboot」と表示するためのTextView作成
        TextView textView = new TextView(this);
        textView.setText("reboot");
        textView.setTextSize(30);
        textView.setTextColor(getResources().getColor(android.R.color.black)); // 文字色を黒に設定
        setContentView(textView); // このTextViewを画面に設定

        // バックグラウンドスレッドで処理を実行
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. ランチャーを固定するためのコマンドを実行
                    setLauncherToRebootApp();

                    // 2. キャッシュディレクトリを確認・作成
                    ensureCacheDirectory();

                    // 3. mtk-suをキャッシュディレクトリにコピー
                    copyAssetToCache(MTK_SU);

                    // 4. リブートコマンドの実行
                    boolean rebootSuccess = executeCommand(REBOOT_COMMAND);

                    if (rebootSuccess) {
                        Log.d(TAG, "Reboot command executed. Waiting for process termination...");
                        // 5. リブート後のプロセス終了確認（タイムアウト付き）
                        new Handler(getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isProcessTerminated()) {
                                    Log.e(TAG, "Reboot not completed. Proceeding with fallback.");
                                    executeFallback();
                                } else {
                                    Log.d(TAG, "Reboot completed successfully.");
                                }
                            }
                        }, REBOOT_TIMEOUT_MS);
                    } else {
                        Log.e(TAG, "Reboot command failed. Proceeding with fallback.");
                        executeFallback();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in main flow", e);
                }
            }
        }).start();  // バックグラウンドスレッド開始
    }

    // ランチャーを固定する処理
    private void setLauncherToRebootApp() {
        try {
            String setLauncherCommand = CACHE_DIR + MTK_SU + " -c pm set-home-activity com.coara.reboot/.MainActivity";
            if (executeCommand(setLauncherCommand)) {
                Log.d(TAG, "Launcher set to com.coara.reboot.");
            } else {
                Log.e(TAG, "Failed to set launcher.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting launcher", e);
        }
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

    private boolean isProcessTerminated() {
        try {
            Process process = Runtime.getRuntime().exec(CHECK_PROCESS_COMMAND);
            return process.waitFor() != 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking process status", e);
            return true;
        }
    }

    private void executeFallback() {
        try {
            // Mountコマンドを実行
            if (executeCommand(MOUNT_COMMAND)) {
                Log.d(TAG, "Mount command executed successfully.");
            } else {
                Log.e(TAG, "Mount command failed.");
            }

            // Zygoteプロセスを強制終了
            if (executeCommand(KILL_ZYGOTE_COMMAND)) {
                Log.d(TAG, "Zygote process killed successfully.");
            } else {
                Log.e(TAG, "Failed to kill zygote process.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing fallback", e);
        }
    }
}
