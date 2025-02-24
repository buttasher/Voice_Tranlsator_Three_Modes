package com.example.voicetranlsator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class CameraTranslator extends AppCompatActivity {

    Spinner spinnerFirst, spinnerSecond;
    EditText etWord, etResult;
    ImageView ivTranslator, ivCopy, ivMic, ivThumb, ivCancel, ivCamera, ivSpeakText, ivSpeakTranslation;
    TextView translationView, textView;
    TextToSpeech textToSpeech;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int REQ_CODE_CAMERA = 101;
    private static final int REQ_CODE_GALLERY = 102;

    private static final String TRANSLATE_API_URL = "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_translator);

        spinnerFirst = findViewById(R.id.spinnerFirstLanguage);
        spinnerSecond = findViewById(R.id.spinnerSecondLanguage);
        etWord = findViewById(R.id.etWord);
        etResult = findViewById(R.id.etResult);
        translationView = findViewById(R.id.translationId);
        textView = findViewById(R.id.textId);
        ivTranslator = findViewById(R.id.ivTranslator);
        ivCopy = findViewById(R.id.ivCopy);
        ivCancel = findViewById(R.id.ivCancel);
        ivMic = findViewById(R.id.ivMic);
        ivThumb = findViewById(R.id.ivThumb);
        ivCamera = findViewById(R.id.ivCamera);
        ivSpeakText = findViewById(R.id.ivSpeakText);
        ivSpeakTranslation = findViewById(R.id.ivSpeakTranslation);

        etResult.setEnabled(false);

        String[] languages = getResources().getStringArray(R.array.languages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerFirst.setAdapter(adapter);
        spinnerSecond.setAdapter(adapter);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        spinnerFirst.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textView.setText(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSecond.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                translationView.setText(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ivCopy.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", etResult.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getBaseContext(), "Translation copied", Toast.LENGTH_SHORT).show();
        });

        ivCamera.setOnClickListener(view -> showImageSourceDialog());

        ivCancel.setOnClickListener(view -> {
            etWord.setText("");
            etResult.setText("");
        });

        ivMic.setOnClickListener(view -> promptSpeechInput());

        ivSpeakText.setOnClickListener(view -> {
            String text = etWord.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Nothing to speak", Toast.LENGTH_SHORT).show();
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        ivSpeakTranslation.setOnClickListener(view -> {
            String translatedText = etResult.getText().toString().trim();
            if (translatedText.isEmpty()) {
                Toast.makeText(this, "Nothing to speak", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedLanguage = spinnerSecond.getSelectedItem().toString();
            Locale languageLocale = getLocaleFromLanguage(selectedLanguage);

            int result = textToSpeech.setLanguage(languageLocale);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported for speech", Toast.LENGTH_SHORT).show();
            } else {
                textToSpeech.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        ivTranslator.setOnClickListener(view -> translateText());
    }

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, REQ_CODE_CAMERA);
            } else if (which == 1) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQ_CODE_GALLERY);
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                etWord.setText(result.get(0));
            }
        } else if ((requestCode == REQ_CODE_CAMERA || requestCode == REQ_CODE_GALLERY) && resultCode == RESULT_OK) {
            try {
                Bitmap imageBitmap;
                if (requestCode == REQ_CODE_CAMERA && data != null) {
                    imageBitmap = (Bitmap) data.getExtras().get("data");
                } else {
                    Uri imageUri = data.getData();
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                }

                InputImage inputImage = InputImage.fromBitmap(imageBitmap, 0);
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(inputImage)
                        .addOnSuccessListener(visionText -> etWord.setText(visionText.getText()))
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to recognize text", Toast.LENGTH_SHORT).show());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private Locale getLocaleFromLanguage(String language) {
        switch (language) {
            case "Arabic": return new Locale("ar");
            case "Urdu": return new Locale("ur");
            case "English": return Locale.ENGLISH;
            case "Spanish": return new Locale("es");
            case "French": return new Locale("fr");
            case "German": return new Locale("de");
            case "Hindi": return new Locale("hi");
            case "Turkish": return new Locale("tr");
            case "Chinese": return Locale.CHINESE;
            case "Japanese": return Locale.JAPANESE;
            case "Korean": return Locale.KOREAN;
            case "Russian": return new Locale("ru");
            default: return Locale.getDefault();
        }
    }

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
                        Toast.makeText(this, "Error in translation", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show());

        queue.add(stringRequest);
    }
}