package com.UniverTranslatevoiceTranslator.UniverTranslate;

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
import com.UniverTranslatevoiceTranslator.MessageAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

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
    AdRequest adRequest;
    private ImageView lastMicClicked;


    AdView adView;

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
        adView       = findViewById(R.id.adView);

        // Setup Spinners
        String[] languages = getResources().getStringArray(R.array.languages);
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerFirst.setAdapter(adapterSpinner);
        int defualtPosition = adapterSpinner.getPosition("English");
        spinnerFirst.setSelection(defualtPosition);

        spinnerSecond.setAdapter(adapterSpinner);
        int defualtPoistion2 = adapterSpinner.getPosition("Spanish");
        spinnerSecond.setSelection(defualtPoistion2);

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
            isUserOneSpeaking = true;  // Set flag for User 1
            promptSpeechInput(spinnerFirst,isUserOneSpeaking);
        });

        // Mic 2 (User Two)
        ivMic2.setOnClickListener(view -> {
            isUserOneSpeaking = false; // Set flag for User 2
            promptSpeechInput(spinnerSecond, isUserOneSpeaking);

        });

        MobileAds.initialize(this);

        adRequest = new AdRequest.Builder().build();

        adView.loadAd(adRequest);
    }

    // Prompt user to speak
    private void promptSpeechInput(Spinner languageSpinner, boolean isUserOne) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Get the correct language for speech recognition
        String selectedLanguage = isUserOne ? spinnerFirst.getSelectedItem().toString() : spinnerSecond.getSelectedItem().toString();
        String languageCode = getLanguageCode(selectedLanguage); // Convert name to language code

        if (languageCode == null) {
            Toast.makeText(this, "Selected language not supported for speech recognition", Toast.LENGTH_SHORT).show();
            return;
        }

        // Debugging: Log selected language for speech recognition
        System.out.println("Speech Recognition Language: " + selectedLanguage + " -> " + languageCode);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now in " + selectedLanguage);

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);

                // Debugging: Log detected speech
                System.out.println("Recognized Speech: " + spokenText);

                // Pass 'isUserOneSpeaking' to ensure correct translation direction
                translateText(spokenText, isUserOneSpeaking);
            }
        }
    }



    // Translate Text using MyMemory API
    private void translateText(String text, boolean isUserOne) {
        try {
            String encodedText = URLEncoder.encode(text, "UTF-8");

            // Correct language switching
            String sourceLangName = isUserOne ? spinnerFirst.getSelectedItem().toString() : spinnerSecond.getSelectedItem().toString();
            String targetLangName = isUserOne ? spinnerSecond.getSelectedItem().toString() : spinnerFirst.getSelectedItem().toString();

            String sourceLanguage = getLanguageCode(sourceLangName);
            String targetLanguage = getLanguageCode(targetLangName);

            if (sourceLanguage == null || targetLanguage == null) {
                Toast.makeText(this, "Language selection error", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = String.format(TRANSLATE_API_URL, encodedText, sourceLanguage, targetLanguage);

            // Debugging: Log API request
            System.out.println("Translation Request: " + url);

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            JSONObject data = jsonResponse.getJSONObject("responseData");
                            String translatedText = data.optString("translatedText", "Translation Failed");

                            // Debugging: Log API response
                            System.out.println("Translated Text: " + translatedText);

                            if (translatedText.isEmpty()) {
                                Toast.makeText(this, "Translation Error", Toast.LENGTH_SHORT).show();
                            } else {
                                addMessageToChat(text, translatedText, isUserOne);
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





    // Map Language Name to Locale
    private String getLanguageCode(String language) {
        switch (language) {
            case "English": return "en-US";
            case "Spanish": return "es-ES";
            case "Urdu": return "ur-PK";  // Confirm this is correct
            case "Hindi": return "hi-IN";
            case "Arabic": return "ar-SA";
            case "Persian": return "fa-IR";
            case "Chinese": return "zh-CN";
            case "Japanese": return "ja-JP";
            default: return null;
        }
    }


    // Add the recognized speech and translation to the chat
    private void addMessageToChat(String originalText, String translatedText, boolean isUserOne) {
        Message message = new Message(originalText, translatedText, isUserOne);
        messagesList.add(message);
        adapter.notifyItemInserted(messagesList.size() - 1);
        recyclerView.smoothScrollToPosition(messagesList.size() - 1);
    }


}
