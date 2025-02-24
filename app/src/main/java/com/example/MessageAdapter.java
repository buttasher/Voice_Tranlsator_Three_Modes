package com.example;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voicetranlsator.Message;
import com.example.voicetranlsator.R;

import java.util.ArrayList;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Message> messagesList;
    private TextToSpeech textToSpeech;
    private String targetLanguage; // Target language for speaking

    // Constructor updated to accept targetLanguage
    public MessageAdapter(Context context, ArrayList<Message> messagesList, String targetLanguage) {
        this.context = context;
        this.messagesList = messagesList;
        this.targetLanguage = targetLanguage;

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR) {
                // Set a default language if needed; this will be overridden on speak
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });
    }

    // Setter for target language, in case it changes
    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    @Override
    public int getItemViewType(int position) {
        return messagesList.get(position).isUserOne() ? 1 : 2;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_user, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_other, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messagesList.get(position);
        holder.tvOriginalText.setText(message.getOriginalText());
        holder.tvTranslatedText.setText(message.getTranslatedText());

        // Align messages dynamically
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams instanceof RecyclerView.LayoutParams) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) layoutParams;
            if (message.isUserOne()) {
                holder.itemView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                params.setMarginStart(20);
                params.setMarginEnd(100);
            } else {
                holder.itemView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                params.setMarginStart(20);
                params.setMarginEnd(100);
            }
            holder.itemView.setLayoutParams(params);
        }

        // Copy translation to clipboard
        holder.ivCopy.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                String copiedText = holder.tvTranslatedText.getText().toString().trim();
                if (!copiedText.isEmpty()) {
                    ClipData clip = ClipData.newPlainText("Copied Text", copiedText);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Translation copied", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "No text to copy!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Clipboard not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete message bubble
        holder.ivCancel.setOnClickListener(view -> {
            messagesList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, messagesList.size());
            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();
        });

        // Speak translation aloud
        holder.ivSpeakTranslation.setOnClickListener(view -> {
            String textToSpeak = holder.tvTranslatedText.getText().toString().trim();
            if (textToSpeak.isEmpty()) {
                Toast.makeText(context, "No text to speak!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use the targetLanguage field passed from ConversationTranslator
            Locale languageLocale = getLocaleFromLanguage(targetLanguage);
            int result = textToSpeech.setLanguage(languageLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "Language not supported for speech", Toast.LENGTH_SHORT).show();
            } else {
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOriginalText, tvTranslatedText;
        ImageView ivCopy, ivCancel, ivSpeakTranslation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginalText = itemView.findViewById(R.id.tvOriginalText);
            tvTranslatedText = itemView.findViewById(R.id.tvTranslatedText);
            ivCopy = itemView.findViewById(R.id.ivCopy);
            ivCancel = itemView.findViewById(R.id.ivCancel);
            ivSpeakTranslation = itemView.findViewById(R.id.ivSpeakTranslation);
        }
    }

    // Map Language Name to Locale
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
            default: return Locale.getDefault(); // Default system language
        }
    }
}
