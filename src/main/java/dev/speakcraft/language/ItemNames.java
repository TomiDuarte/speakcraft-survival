package dev.speakcraft.language;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ItemNames {
    private static final Map<String, Map<String, String>> NAMES = load();
    private ItemNames() { }
    private static Map<String, Map<String, String>> load() {
        var all = new HashMap<String, Map<String, String>>();
        for (String language : List.of("es", "de", "ja")) {
            try (InputStream input = ItemNames.class.getResourceAsStream("/data/speakcraft/names/" + language + ".json")) {
                if (input == null) throw new IllegalStateException("Missing official names: " + language);
                JsonObject json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                var entries = new HashMap<String, String>();
                json.entrySet().forEach(e -> entries.put(e.getKey(), e.getValue().getAsString()));
                all.put(language, Map.copyOf(entries));
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }
        return Map.copyOf(all);
    }
    public static Optional<String> name(String translationKey, String language) {
        return Optional.ofNullable(NAMES.getOrDefault(language, Map.of()).get(translationKey));
    }
    public static List<String> answers(String translationKey, String language) {
        // Only official names; romaji is a reading aid, not an alternative transcription.
        return name(translationKey, language).map(List::of).orElse(List.of());
    }
    public static String reading(String translationKey, String language) {
        return language.equals("ja") ? name(translationKey, language)
            .map(name -> JapaneseReadings.reading(translationKey, name)).orElse("") : "";
    }
}
