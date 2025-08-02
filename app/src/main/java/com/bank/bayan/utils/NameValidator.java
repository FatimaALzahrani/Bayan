package com.bank.bayan.utils;

import android.util.Log;

public class NameValidator {

    private static final String TAG = "NameValidator";

    private static final int MIN_NAME_WORDS = 4;

    public static ValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new ValidationResult(false, "الاسم فارغ", 0);
        }

        String cleanedName = name.trim().replaceAll("\\s+", " ");

        String[] words = cleanedName.split("\\s+");
        int wordCount = words.length;

        Log.d(TAG, "Validating name: '" + cleanedName + "' - Word count: " + wordCount);

        if (wordCount < MIN_NAME_WORDS) {
            String message = getInsufficientWordsMessage(wordCount);
            return new ValidationResult(false, message, wordCount);
        }

        for (String word : words) {
            if (!isValidWord(word)) {
                return new ValidationResult(false, "الاسم يحتوي على أحرف غير صحيحة: " + word, wordCount);
            }
        }

        for (String word : words) {
            if (word.length() < 2) {
                return new ValidationResult(false, "إحدى الكلمات قصيرة جداً: " + word, wordCount);
            }
        }

        return new ValidationResult(true, "الاسم صحيح", wordCount);
    }

    private static boolean isValidWord(String word) {
        if (word == null || word.length() < 2) {
            return false;
        }

        for (char c : word.toCharArray()) {
            if (!isValidCharacter(c)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidCharacter(char c) {
        if ((c >= '\u0600' && c <= '\u06FF') ||
                (c >= '\u0750' && c <= '\u077F')) {
            return true;
        }

        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return true;
        }

        return c == '-' || c == '\'' || c == '.';
    }

    private static String getInsufficientWordsMessage(int wordCount) {
        switch (wordCount) {
            case 1:
                return "الاسم الذي قلته يحتوي على كلمة واحدة فقط. يرجى قول اسمك الرباعي كاملاً (الاسم الأول، اسم الأب، اسم الجد، اسم العائلة)";
            case 2:
                return "الاسم الذي قلته يحتوي على كلمتين فقط. يرجى قول اسمك الرباعي كاملاً (الاسم الأول، اسم الأب، اسم الجد، اسم العائلة)";
            case 3:
                return "الاسم الذي قلته ثلاثي. يرجى قول اسمك الرباعي كاملاً (الاسم الأول، اسم الأب، اسم الجد، اسم العائلة)";
            default:
                return "عدد كلمات الاسم غير كافي. يرجى قول اسمك الرباعي كاملاً";
        }
    }

    public static String cleanName(String name) {
        if (name == null) {
            return "";
        }

        return name.trim().replaceAll("\\s+", " ");
    }

    public static boolean namesMatch(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }

        String clean1 = cleanName(name1).toLowerCase();
        String clean2 = cleanName(name2).toLowerCase();

        return clean1.equals(clean2);
    }

    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        public final int wordCount;

        public ValidationResult(boolean isValid, String message, int wordCount) {
            this.isValid = isValid;
            this.message = message;
            this.wordCount = wordCount;
        }

        @Override
        public String toString() {
            return "ValidationResult{valid=" + isValid + ", message='" + message + "', words=" + wordCount + "}";
        }
    }
}