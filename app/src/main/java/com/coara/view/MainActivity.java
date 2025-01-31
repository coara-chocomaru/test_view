<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <!-- WebView -->
    <WebView
        android:id="@+id/webView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/searchQuery"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="16dp" />

    <!-- Search Query Field -->
    <EditText
        android:id="@+id/searchQuery"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:hint="Search"
        android:textColor="#000000"
        android:padding="10dp"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="8dp"
        android:backgroundTint="#cccccc"
        app:layout_constraintTop_toBottomOf="@id/webView"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Start Date Field -->
    <EditText
        android:id="@+id/startDate"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:hint="Start Date (YYYY/MM/DD)"
        android:textColor="#000000"
        android:padding="10dp"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="8dp"
        android:backgroundTint="#cccccc"
        app:layout_constraintTop_toBottomOf="@id/searchQuery"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- End Date Field -->
    <EditText
        android:id="@+id/endDate"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:hint="End Date (YYYY/MM/DD)"
        android:textColor="#000000"
        android:padding="10dp"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="8dp"
        android:backgroundTint="#cccccc"
        app:layout_constraintTop_toBottomOf="@id/startDate"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Buttons: Screenshot, Invert, Search -->
    <Button
        android:id="@+id/screenshotButton"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Take Screenshot"
        android:background="#2196F3"
        android:textColor="#ffffff"
        android:layout_marginTop="16dp"
        app:layout_constraintTop_toBottomOf="@id/endDate"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <Button
        android:id="@+id/invertButton"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Invert Colors"
        android:background="#FF9800"
        android:textColor="#ffffff"
        android:layout_marginTop="8dp"
        app:layout_constraintTop_toBottomOf="@id/screenshotButton"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <Button
        android:id="@+id/searchButton"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Search"
        android:background="#4CAF50"
        android:textColor="#ffffff"
        android:layout_marginTop="8dp"
        app:layout_constraintTop_toBottomOf="@id/invertButton"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Bottom Navigation -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNav"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        app:layout_constraintBottom_toBottomOf="parent"
        android:background="?android:attr/windowBackground"
        app:menu="@menu/bottom_nav_menu" />

</androidx.constraintlayout.widget.ConstraintLayout>
