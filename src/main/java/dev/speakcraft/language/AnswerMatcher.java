package dev.speakcraft.language;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;

/** Text-transcription tolerance, not a phonetic assessment. No accents/kana are discarded. */
public final class AnswerMatcher {
    private AnswerMatcher() { }
    public static String normalize(String text, String language) {
        String value = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{Z}\\s]+", " ").strip();
        // Vosk inserts segmentation spaces between Japanese words. Preserve every actual character.
        return language.equals("ja") ? value.replace(" ", "") : value;
    }
    public static boolean matches(String text, Collection<String> answers, String language, double tolerance) {
        if (!Double.isFinite(tolerance) || tolerance < 0 || tolerance > .25) return false;
        String spoken = normalize(text, language);
        if (spoken.isBlank() || spoken.length() > 256) return false;
        for (String answer : answers) {
            String expected = normalize(answer, language);
            if (spoken.equals(expected)) return true;
            int[] a = spoken.codePoints().toArray(), b = expected.codePoints().toArray();
            int max = Math.max(a.length, b.length);
            // Short words remain exact to avoid accepting another material/item.
            if (Math.min(a.length, b.length) >= 5 && distance(a, b) <= Math.floor(max * tolerance)) return true;
        }
        return false;
    }
    static int distance(int[] a, int[] b) {
        int[] row = new int[b.length + 1];
        for (int j = 0; j <= b.length; j++) row[j] = j;
        for (int i = 1; i <= a.length; i++) {
            int diagonal = row[0]; row[0] = i;
            for (int j = 1; j <= b.length; j++) {
                int above = row[j];
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), diagonal + (a[i - 1] == b[j - 1] ? 0 : 1));
                diagonal = above;
            }
        }
        return row[b.length];
    }
}
