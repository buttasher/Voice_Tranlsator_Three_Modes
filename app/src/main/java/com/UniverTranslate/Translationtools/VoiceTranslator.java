package com.UniverTranslate.Translationtools;

import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.UniverTranslate.Translationtools.VoiceTranslator;
import com.UniverTranslateTranslationtools.R;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceTranslator extends AppCompatActivity {

    Spinner spinnerFirst, spinnerSecond;
    EditText etWord, etResult;
    ImageView ivTranslator, ivCopy, ivMic,ivCancel, ivPaste, ivSpeakText, ivSpeakTranslation;
    TextView translationView, textView;
    TextToSpeech textToSpeech;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    AdRequest adRequest;

    AdView adView;


    private static final String TRANSLATE_API_URL = "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s"; // Free API

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_translator);


        // Initialize UI Elements
        spinnerFirst = findViewById(R.id.spinnerFirstLanguage);
        spinnerSecond = findViewById(R.id.spinnerSecondLanguage);
        etWord = findViewById(R.id.etWord);
        etResult = findViewById(R.id.etResult);
        translationView = findViewById(R.id.translationId);
        textView = findViewById(R.id.textId);
        ivTranslator = findViewById(R.id.ivTranslator); // Translation Button
        ivCopy = findViewById(R.id.ivCopy);
        ivCancel = findViewById(R.id.ivCancel);
        ivMic = findViewById(R.id.ivMic);
        ivPaste = findViewById(R.id.ivPaste);
        ivSpeakText = findViewById(R.id.ivSpeakText);
        ivSpeakTranslation = findViewById(R.id.ivSpeakTranslation);
        adView             = findViewById(R.id.adView);


        etResult.setEnabled(false);

        // Setup Spinner Adapter
        String[] languages = getResources().getStringArray(R.array.languages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerFirst.setAdapter(adapter);

        int defaultPosition = adapter.getPosition("English");
        spinnerFirst.setSelection(defaultPosition);

        spinnerSecond.setAdapter(adapter);
        int defualtPosition2 = adapter.getPosition("Spanish");
        spinnerSecond.setSelection(defualtPosition2);




        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault()); // Default language
            }
        });
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

        // Handle Language Selection for Spinner 1 (From Language)
        spinnerFirst.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Handle Language Selection for Spinner 2 (To Language)
        spinnerSecond.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Copy Translation to Clipboard
        ivCopy.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", etResult.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getBaseContext(), "Translation copied", Toast.LENGTH_SHORT).show();
        });

        // Paste Text from Clipboard
        ivPaste.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            try {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                etWord.setText(text);
            } catch (Exception ignored) {}
        });

        // Cancel Text
        ivCancel.setOnClickListener(view -> {
            etWord.setText("");
            etResult.setText("");
        });

        // Speak Text
        ivMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });

        // Click listener for ivSpeakText (Speak the Original Text)
        ivSpeakText.setOnClickListener(view -> {
            String text = etWord.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Nothing to speak", Toast.LENGTH_SHORT).show();
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        // Click listener for ivSpeakTranslation (Speak the Translated Text)
        // Speak the Translated Text with Correct Language Locale
        ivSpeakTranslation.setOnClickListener(view -> {
            String translatedText = etResult.getText().toString().trim();
            if (translatedText.isEmpty()) {
                Toast.makeText(this, "Nothing to speak", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the language code from the spinner
            String selectedLanguage = spinnerSecond.getSelectedItem().toString();
            String languageCode = getSpeechLanguageCode (selectedLanguage); // Convert language name to code

            // Set the TTS language
            Locale languageLocale = new Locale(languageCode);
            int result = textToSpeech.setLanguage(languageLocale);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Speech synthesis not supported for " + selectedLanguage, Toast.LENGTH_SHORT).show();
            } else {
                textToSpeech.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });




        // Translation Button Click (Triggers Translation)
        // Translation Button Click (Triggers Translation)
        ivTranslator.setOnClickListener(view -> {
            // Hide the keyboard
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }

            // Perform translation
            translateText();
        });

        MobileAds.initialize(this);

        adRequest = new AdRequest.Builder().build();

        adView.loadAd(adRequest);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                // Set text directly without modification
                etWord.setText(result.get(0));

                // Optional: Set input direction for RTL languages
                String langCode = getSpeechLanguageCode(spinnerFirst.getSelectedItem().toString());
                if (langCode.startsWith("ar") || langCode.startsWith("ur") || langCode.startsWith("fa")) {
                    etWord.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
                } else {
                    etWord.setTextDirection(View.TEXT_DIRECTION_LTR);
                }
            }
        }
    }


    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Get selected language from spinner
        String selectedLanguage = spinnerFirst.getSelectedItem().toString();
        String languageCode = getSpeechLanguageCode(selectedLanguage);

        // Additional configuration for better recognition
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak in " + selectedLanguage);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode);

        try {
            // Check if there's an app that can handle this request
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            } else {
                showLanguageSupportError(selectedLanguage);
            }
        } catch (ActivityNotFoundException e) {
            showLanguageSupportError(selectedLanguage);
        }
    }

    private void showLanguageSupportError(String language) {
        String message = "Could not find speech support for " + language + ".\n" +
                "1. Check if Google app is installed\n" +
                "2. Verify language pack is downloaded\n" +
                "3. Try in online mode";

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Optional: Open language settings
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS));
        } catch (ActivityNotFoundException ignored) {
        }
    }




    // Map Language Name to Locale
    private String getSpeechLanguageCode(String language) {
        // Updated with proper BCP-47 language codes and regions
        switch (language) {
            case "Urdu": return "ur-PK";  // Urdu (Pakistan)
            case "Hindi": return "hi-IN";  // Hindi (India)
            case "Arabic": return "ar-SA"; // Arabic (Saudi Arabia)
            case "Spanish": return "es-ES"; // Spanish (Spain)
            case "French": return "fr-FR";  // French (France)
            case "German": return "de-DE";  // German (Germany)
            case "Chinese": return "zh-CN"; // Chinese (Simplified, China)
            case "Japanese": return "ja-JP"; // Japanese (Japan)
            case "Russian": return "ru-RU";  // Russian (Russia)
            case "Persian": return "fa-IR";  // Persian (Iran)
            default: return "en-US";       // Default to English (US)
        }
    }




    // Translation Logic Using API
    private void translateText() {
        String inputText = etWord.getText().toString().trim();
        if (inputText.isEmpty()) {
            Toast.makeText(this, "Enter text to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromLanguage = Methods.getLanguageCode(spinnerFirst.getSelectedItem().toString());
        String toLanguage = Methods.getLanguageCode(spinnerSecond.getSelectedItem().toString());

        if (fromLanguage == null || toLanguage == null) {
            Toast.makeText(this, "Invalid language selection", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = String.format(TRANSLATE_API_URL, inputText, fromLanguage, toLanguage);
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String translatedText = jsonObject.getJSONObject("responseData").getString("translatedText");
                        etResult.setText(translatedText);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(VoiceTranslator.this, "Error in translation", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(VoiceTranslator.this, "Failed to connect", Toast.LENGTH_SHORT).show());

        queue.add(stringRequest);
    }
}
