# Androidクラスを保持
-keep class android.** { *; }
-keep interface android.** { *; }

# Logメソッドの最適化を無効化
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# MainActivityクラスを保持
-keep class com.coara.view.MainActivity { *; }

# リソースファイルの内容とファイル名を調整
-adaptresourcefilecontents **.xml
-adaptresourcefilenames **.png

# Serializable実装クラスの保持
-keepnames class * implements java.io.Serializable {
    static final long serialVersionUID;
}

# すべてのクラスの保持（必要に応じて詳細化）
-keep class ** { *; }

# アノテーションや例外、内部クラスなどの属性を保持
-keepattributes *Annotation*
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, EnclosingMethod

# クラス名を変更しない
-classobfuscationdictionary obfuscation-dictionary.txt

# 最適化やシュリンクを無効化
-dontshrink
-dontoptimize

# クラスアクセス修正を許可
-allowaccessmodification

# クラスのリパッケージを無効化
-repackageclasses ''

# クラス文字列を調整
-adaptclassstrings

# クラスメンバー（getter, setter, lambdaメソッド）を保持
-keepclassmembers class * {
    *;
    public void set*(...);
    public void get*(...);
    void lambda*(...);
}

# JSON関連の設定
-keep class org.json.** { *; }  # org.json クラスを保持
-keep class com.google.gson.** { *; }  # Gson ライブラリを使用している場合

# デバッグ用のプリビリファイを無効化
-dontpreverify
