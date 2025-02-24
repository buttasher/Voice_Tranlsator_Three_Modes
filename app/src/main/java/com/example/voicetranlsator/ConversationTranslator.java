package com.example.voicetranlsator;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.MessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;

public class ConversationTranslator extends AppCompatActivity {
    Spinner spinnerFirst, spinnerSecond;
    ImageView ivMic1, ivMic2;
    RecyclerView recyclerView;
    MessageAdapter adapter;
    ArrayList<Message> messagesList;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private boolean isUserOneSpeaking = true;

    private static final String TRANSLATE_API_URL = "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_translator);

        // Set Status Bar Color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        // Initialize UI Elements
        ivMic1 = findViewById(R.id.ivMicFirst);
        ivMic2 = findViewById(R.id.ivMicSecond);
        spinnerFirst = findViewById(R.id.spinnerFirstLanguage);
        spinnerSecond = findViewById(R.id.spinnerSecondLanguage);
        recyclerView = findViewById(R.id.recyclerViewTranslations);

        // Setup Spinners
        String[] languages = getResources().getStringArray(R.array.languages);
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerFirst.setAdapter(adapterSpinner);
        spinnerSecond.setAdapter(adapterSpinner);

        // Setup RecyclerView and MessageAdapter (pass initial target language)
        messagesList = new ArrayList<>();
        String targetLang = spinnerSecond.getSelectedItem().toString();
        adapter = new MessageAdapter(this, messagesList, targetLang);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Update adapter when target language changes
        spinnerSecond.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newTargetLang = parent.getItemAtPosition(position).toString();
                adapter.setTargetLanguage(newTargetLang);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Mic 1 (User One)
        ivMic1.setOnClickListener(view -> {
            isUserOneSpeaking = true;
            promptSpeechInput();
        });

        // Mic 2 (User Two)
        ivMic2.setOnClickListener(view -> {
            isUserOneSpeaking = false;
            promptSpeechInput();
        });
    }

    // Prompt user to speak
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Determine language code based on who is speaking
        String selectedLanguage = isUserOneSpeaking ?
                spinnerFirst.getSelectedItem().toString() : spinnerSecond.getSelectedItem().toString();
        String languageCode = Methods.getLanguageCode(selectedLanguage);

        if (languageCode == null) {
            Toast.makeText(this, "Unsupported Language", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "Speech recognition not supported on this device",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                translateText(spokenText);
            }
        }
    }

    // Translate Text using MyMemory API
    private void translateText(String text) {
        try {
            String encodedText = URLEncoder.encode(text, "UTF-8");

            // Get language codes from spinner selections
            String sourceLanguage = Methods.getLanguageCode(spinnerFirst.getSelectedItem().toString());
            String targetLanguage = Methods.getLanguageCode(spinnerSecond.getSelectedItem().toString());

            if (sourceLanguage == null || targetLanguage == null) {
                Toast.makeText(this, "Language selection error", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = String.format(TRANSLATE_API_URL, encodedText, sourceLanguage, targetLanguage);

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            JSONObject data = jsonResponse.getJSONObject("responseData");
                            String translatedText = data.optString("translatedText", "Translation Failed");

                            if (translatedText.isEmpty()) {
                                Toast.makeText(this, "Translation Error", Toast.LENGTH_SHORT).show();
                            } else {
                                addMessageToChat(text, translatedText);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Invalid API Response", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "Translation API Error", Toast.LENGTH_SHORT).show();
                    });
            queue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Encoding Error", Toast.LENGTH_SHORT).show();
        }
    }

    // Add the recognized speech and translation to the chat
    private void addMessageToChat(String originalText, String translatedText) {
        Message message = new Message(originalText, translatedText, isUserOneSpeaking);
        messagesList.add(message);
        adapter.notifyItemInserted(messagesList.size() - 1);
        recyclerView.smoothScrollToPosition(messagesList.size() - 1);
    }
}
