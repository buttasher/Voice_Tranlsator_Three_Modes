package com.UniverTranslatevoiceTranslator.UniverTranslate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );


         new Handler().postDelayed(() -> {
             SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
             boolean isFirstTime = preferences.getBoolean("isFirstTime", true);

             if (isFirstTime) {
                 // Show MainActivity (Onboarding)
                 startActivity(new Intent(SplashScreen.this, MainActivity.class));
             } else {
                 // Show HomeActivity directly
                 startActivity(new Intent(SplashScreen.this, HomeActivity.class));
             }
             finish();
        }, SPLASH_TIME_OUT);


    }
}
