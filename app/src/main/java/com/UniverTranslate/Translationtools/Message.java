package com.UniverTranslate.Translationtools;

public class Message {
    private String originalText;
    private String translatedText;
    private boolean isUserOne; // Determines which chat bubble to use

    public Message(String originalText, String translatedText, boolean isUserOne) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.isUserOne = isUserOne;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public boolean isUserOne() {
        return isUserOne;
    }


}
