package com.coara.apkload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import dalvik.system.DexClassLoader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ProxyActivity extends Activity {
    private static final String TAG = "ProxyActivity";
    private Activity externalActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String apkPath = getIntent().getStringExtra("apkPath");
        if (apkPath != null) {
            loadExternalApk(apkPath);
        } else {
            Log.e(TAG, "APK path is null");
        }
    }

    private void loadExternalApk(String apkPath) {
        try {
            File dexFile = new File(apkPath, "classes.dex");

            if (!dexFile.exists()) {
                Log.e(TAG, "Dex file not found at: " + dexFile.getAbsolutePath());
                return;
            }

            File optimizedDir = new File(getCacheDir(), "dex_opt");
            if (!optimizedDir.exists()) optimizedDir.mkdirs();

            // DexClassLoaderを使用してクラスをロード
            DexClassLoader classLoader = new DexClassLoader(
                    dexFile.getAbsolutePath(), optimizedDir.getAbsolutePath(), null, getClassLoader()
            );

            // 外部APKのMainActivityクラスをロード
            Class<?> clazz = classLoader.loadClass("com.example.plugin.MainActivity");
            Constructor<?> constructor = clazz.getConstructor();
            Object instance = constructor.newInstance();

            if (instance instanceof Activity) {
                externalActivity = (Activity) instance;

                // 'attach()' メソッドを使ってコンテキストをアタッチ
                Method attachMethod = Activity.class.getDeclaredMethod("attach", Context.class);
                attachMethod.setAccessible(true);
                attachMethod.invoke(externalActivity, this);

                // それから外部アクティビティを開始
                externalActivity.onCreate(null);

                // アクティビティを現在のアクティビティとして設定
                startActivity(new Intent(this, externalActivity.getClass()));
                finish();  // ProxyActivityは終了
            } else {
                Log.e(TAG, "Loaded class is not an instance of Activity");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading APK: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
