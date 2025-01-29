package com.coara.apkload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ProxyActivity extends Activity {
    private Activity externalActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String apkPath = getIntent().getStringExtra("apkPath");
        if (apkPath != null) loadExternalApk(apkPath);
    }

    private void loadExternalApk(String apkPath) {
        try {
            File dexFile = new File(apkPath, "classes.dex");
            File optimizedDir = new File(getCacheDir(), "dex_opt");
            optimizedDir.mkdirs();

            DexClassLoader classLoader = new DexClassLoader(
                dexFile.getAbsolutePath(), optimizedDir.getAbsolutePath(), null, getClassLoader()
            );

            Class<?> clazz = classLoader.loadClass("com.example.plugin.MainActivity");
            Constructor<?> constructor = clazz.getConstructor();
            Object instance = constructor.newInstance();

            if (instance instanceof Activity) {
                externalActivity = (Activity) instance;
                Method attachMethod = Activity.class.getDeclaredMethod(
                    "attach", Context.class
                );
                attachMethod.setAccessible(true);
                attachMethod.invoke(externalActivity, this);
                externalActivity.onCreate(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
