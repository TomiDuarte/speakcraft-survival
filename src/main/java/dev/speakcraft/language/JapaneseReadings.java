package dev.speakcraft.language;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Precomputed dictionary readings: no tokenizer, model download or transliteration on the game thread. */
public final class JapaneseReadings {
    private record Entry(String name, String romaji) { }
    private static final Map<String, Entry> READINGS = load();
    private JapaneseReadings() { }

    private static Map<String, Entry> load() {
        try (InputStream input = JapaneseReadings.class.getResourceAsStream("/data/speakcraft/readings/ja.json")) {
            if (input == null) throw new IllegalStateException("Missing Japanese readings");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var entries = new HashMap<String, Entry>();
            json.entrySet().forEach(e -> {
                var value = e.getValue().getAsJsonObject();
                entries.put(e.getKey(), new Entry(value.get("name").getAsString(), value.get("romaji").getAsString()));
            });
            return Map.copyOf(entries);
        } catch (IOException error) { throw new UncheckedIOException(error); }
    }

    public static String reading(String translationKey, String officialName) {
        Entry entry = READINGS.get(translationKey);
        // Avoid showing a stale reading if names change in a future Minecraft release.
        return entry != null && entry.name().equals(officialName) ? entry.romaji() : "";
    }
}
