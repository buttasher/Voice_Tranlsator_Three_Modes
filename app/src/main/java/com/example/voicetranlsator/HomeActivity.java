package com.example.voicetranlsator;


import android.content.Intent;

import android.graphics.Color;

import android.os.Build;
import android.os.Bundle;

import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;


public class HomeActivity extends AppCompatActivity {

    CardView cardView1, cardView2, cardView3;
    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        cardView1 = findViewById(R.id.card_voiceTranslator);
        cardView2 = findViewById(R.id.card_cameraTranslate);
        cardView3 = findViewById(R.id.card_conversation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE); // Set status bar to white

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // Dark icons
            }
        }

        cardView1.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, VoiceTranslator.class);
            startActivity(intent);
        });


        cardView2.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, CameraTranslator.class);
            startActivity(intent);
        });

        cardView3.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, ConversationTranslator.class);
            startActivity(intent);
        });
    }






    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
}
