package dev.speakcraft.language;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemNamesTest {
    @Test void officialDiamondNamesAreIndependentOfGameUiLanguage() {
        assertEquals("Diamante", ItemNames.name("item.minecraft.diamond", "es").orElseThrow());
        assertEquals("Diamant", ItemNames.name("item.minecraft.diamond", "de").orElseThrow());
        assertEquals("ダイヤモンド", ItemNames.name("item.minecraft.diamond", "ja").orElseThrow());
        assertEquals("daiyamondo", ItemNames.reading("item.minecraft.diamond", "ja"));
        assertFalse(ItemNames.answers("item.minecraft.diamond", "ja").contains("daiyamondo"));
    }
    @Test void unsupportedModdedItemDoesNotInventATranslation() {
        assertTrue(ItemNames.name("item.example.untranslated", "es").isEmpty());
        assertTrue(ItemNames.answers("item.example.untranslated", "es").isEmpty());
        assertEquals("", ItemNames.reading("item.example.untranslated", "ja"));
    }
    @Test void commonSurvivalKanjiHaveLatinReadings() {
        assertEquals("ooku no genboku", ItemNames.reading("block.minecraft.oak_log", "ja"));
        assertEquals("ooku no itazai", ItemNames.reading("block.minecraft.oak_planks", "ja"));
        assertEquals("sagyoudai", ItemNames.reading("block.minecraft.crafting_table", "ja"));
        assertEquals("ki no tsuruhashi", ItemNames.reading("item.minecraft.wooden_pickaxe", "ja"));
        assertEquals("tetsu ingotto", ItemNames.reading("item.minecraft.iron_ingot", "ja"));
        assertEquals("kusatta niku", ItemNames.reading("item.minecraft.rotten_flesh", "ja"));
    }
    @Test void readingsAreOnlyHintsAndDoNotReplaceJapaneseAnswers() {
        assertEquals("", ItemNames.reading("block.minecraft.oak_log", "es"));
        assertEquals("", JapaneseReadings.reading("block.minecraft.oak_log", "changed name"));
        assertEquals(java.util.List.of("オークの原木"), ItemNames.answers("block.minecraft.oak_log", "ja"));
        assertFalse(AnswerMatcher.matches("ooku no genboku", ItemNames.answers("block.minecraft.oak_log", "ja"), "ja", .25));
    }
    @Test void bundledReadingsAreLatinAndMatchOfficialNames() throws Exception {
        try (var input = getClass().getResourceAsStream("/data/speakcraft/readings/ja.json")) {
            assertNotNull(input);
            var entries = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(entries.size() > 2700);
            for (var entry : entries.entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                assertEquals(ItemNames.name(entry.getKey(), "ja").orElseThrow(), value.get("name").getAsString());
                String reading = ItemNames.reading(entry.getKey(), "ja");
                assertFalse(reading.isBlank(), entry.getKey());
                assertTrue(reading.matches("[a-z0-9 '()]+"), entry.getKey() + ": " + reading);
            }
        }
    }
}
