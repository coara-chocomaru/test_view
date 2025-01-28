package com.coara.reboot;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

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

    public static void main(Context context) {
        try {
            // 1. キャッシュディレクトリの作成確認
            ensureCacheDirectory();

            // 2. mtk-suをキャッシュディレクトリにコピー
            copyAssetToCache(context, MTK_SU);

            // 3. リブートコマンドの実行
            boolean rebootSuccess = executeCommand(REBOOT_COMMAND);

            if (rebootSuccess) {
                Log.d(TAG, "Reboot command executed. Waiting for process termination...");
                // 4. リブートプロセスが終了するかタイムアウトを待つ
                new Handler().postDelayed(() -> {
                    if (!isProcessTerminated()) {
                        Log.e(TAG, "Reboot not completed. Proceeding with fallback.");
                        executeFallback();
                    } else {
                        Log.d(TAG, "Reboot completed successfully.");
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

    // キャッシュディレクトリが存在しない場合に作成
    private static void ensureCacheDirectory() {
        File cacheDir = new File(CACHE_DIR);
        if (!cacheDir.exists()) {
            if (cacheDir.mkdirs()) {
                Log.d(TAG, "Cache directory created: " + CACHE_DIR);
            } else {
                Log.e(TAG, "Failed to create cache directory.");
                throw new RuntimeException("Cache directory creation failed.");
            }
        }
    }

    // アセットファイルをキャッシュディレクトリにコピー
    private static void copyAssetToCache(Context context, String fileName) throws Exception {
        File outFile = new File(CACHE_DIR + fileName);
        if (!outFile.exists()) {
            InputStream in = context.getAssets().open(fileName);
            FileOutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            // 実行権限を付与
            if (!outFile.setExecutable(true, false)) {
                Log.e(TAG, "Failed to set executable permission for: " + outFile.getAbsolutePath());
                throw new RuntimeException("Failed to set executable permission.");
            }

            Log.d(TAG, "Copied " + fileName + " to cache with executable permission.");
        } else {
            Log.d(TAG, fileName + " already exists in cache.");
        }
    }

    // 任意のコマンドを実行
    private static boolean executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            Log.e(TAG, "Command execution failed: " + command, e);
            return false;
        }
    }

    // プロセスが終了しているか確認
    private static boolean isProcessTerminated() {
        try {
            Process process = Runtime.getRuntime().exec(CHECK_PROCESS_COMMAND);
            process.waitFor();
            return process.exitValue() != 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking process status", e);
            return true; // 確認できない場合、終了したと仮定
        }
    }

    // フォールバック処理の実行
    private static void executeFallback() {
        try {
            // MOUNT コマンドの実行
            if (executeCommand(MOUNT_COMMAND)) {
                Log.d(TAG, "Mount command executed successfully.");
            } else {
                Log.e(TAG, "Mount command failed.");
            }

            // zygote プロセスを終了
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
