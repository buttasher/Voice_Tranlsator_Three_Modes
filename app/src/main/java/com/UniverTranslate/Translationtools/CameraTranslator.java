package com.UniverTranslate.Translationtools;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.UniverTranslate.Translationtools.CameraTranslator;
import com.UniverTranslateTranslationtools.R;
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
import java.util.Locale;

public class CameraTranslator extends AppCompatActivity {

    Spinner spinnerFirst, spinnerSecond;
    EditText etWord, etResult;
    ImageView ivTranslator, ivCopy, ivCancel, ivCamera, ivSpeakText, ivSpeakTranslation;
    TextView translationView, textView;
    TextToSpeech textToSpeech;
    private static final int REQ_CODE_CAMERA = 101;
    private static final int REQ_CODE_GALLERY = 102;
    private static final int CAMERA_PERMISSION_CODE = 200;

    private static final String TRANSLATE_API_URL = "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_translator);

        showImageSourceDialog();
        spinnerFirst = findViewById(R.id.spinnerFirstLanguage);
        spinnerSecond = findViewById(R.id.spinnerSecondLanguage);
        etWord = findViewById(R.id.etWord);
        etResult = findViewById(R.id.etResult);
        translationView = findViewById(R.id.translationId);
        textView = findViewById(R.id.textId);
        ivTranslator = findViewById(R.id.ivTranslator);
        ivCopy = findViewById(R.id.ivCopy);
        ivCancel = findViewById(R.id.ivCancel);
        ivCamera = findViewById(R.id.ivCamera);
        ivSpeakText = findViewById(R.id.ivSpeakText);
        ivSpeakTranslation = findViewById(R.id.ivSpeakTranslation);

        etResult.setEnabled(false);

        String[] languages = getResources().getStringArray(R.array.languages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);

        spinnerFirst.setAdapter(adapter);
        spinnerFirst.setSelection(adapter.getPosition("English"));

        spinnerSecond.setAdapter(adapter);
        spinnerSecond.setSelection(adapter.getPosition("Spanish"));

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
            String languageCode = getSpeechLanguageCode(selectedLanguage);

            Locale languageLocale = new Locale(languageCode);
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
                checkCameraPermissionAndOpenCamera(); // Runtime permission check
            } else if (which == 1) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQ_CODE_GALLERY);
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCameraIntent();
            } else {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            }
        } else {
            openCameraIntent();
        }
    }

    private void openCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQ_CODE_CAMERA);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraIntent();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap imageBitmap = null;
            if (requestCode == REQ_CODE_CAMERA && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    imageBitmap = (Bitmap) extras.get("data");
                }
            } else if (requestCode == REQ_CODE_GALLERY && data != null) {
                Uri imageUri = data.getData();
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (imageBitmap != null) {
                processTextRecognition(imageBitmap);
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processTextRecognition(Bitmap imageBitmap) {
        InputImage inputImage = InputImage.fromBitmap(imageBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(inputImage)
                .addOnSuccessListener(visionText -> {
                    String recognizedText = visionText.getText();
                    if (recognizedText.isEmpty()) {
                        Toast.makeText(this, "No text detected", Toast.LENGTH_SHORT).show();
                    } else {
                        etWord.setText(recognizedText);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to recognize text", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }



    private String getSpeechLanguageCode(String language) {
        switch (language) {
            case "Urdu": return "ur-PK";
            case "Hindi": return "hi-IN";
            case "Arabic": return "ar-SA";
            case "Spanish": return "es-ES";
            case "French": return "fr-FR";
            case "German": return "de-DE";
            case "Chinese": return "zh-CN";
            case "Japanese": return "ja-JP";
            case "Russian": return "ru-RU";
            case "Persian": return "fa-IR";
            default: return "en-US";
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
                        Toast.makeText(CameraTranslator.this, "Error in translation", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(CameraTranslator.this, "Failed to connect", Toast.LENGTH_SHORT).show());

        queue.add(stringRequest);
    }
}
