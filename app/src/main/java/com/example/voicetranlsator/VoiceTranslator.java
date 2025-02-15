package com.example.voicetranlsator;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class VoiceTranslator extends AppCompatActivity {

    Spinner spinnerFirst, spinnerSecond;
    EditText etWord, etResult;
    TextView transalationView ,textView;
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.voice_translator);
        spinnerFirst = findViewById(R.id.spinnerFirstLanguage);
        spinnerSecond = findViewById(R.id.spinnerSecondLanguage);
        etWord = findViewById(R.id.etWord);
        etResult = findViewById(R.id.etResult);
        transalationView = findViewById(R.id.translationId);
        textView = findViewById(R.id.textId);

        etResult.setEnabled(false);

        String[] languages = getResources().getStringArray(R.array.languages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);

        spinnerFirst.setAdapter(adapter);
        spinnerSecond.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE); // Set status bar to white

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // Dark icons
            }
        }
        // Handle Selection for First Spinner
        spinnerFirst.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = parent.getItemAtPosition(position).toString();
                textView.setText(selectedLanguage); // Set selected language to EditText 1
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Handle Selection for Second Spinner
        spinnerSecond.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = parent.getItemAtPosition(position).toString();
                transalationView.setText(selectedLanguage); // Set selected language to EditText 2
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }
}
