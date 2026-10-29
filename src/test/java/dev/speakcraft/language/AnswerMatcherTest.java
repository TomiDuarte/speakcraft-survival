package dev.speakcraft.language;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AnswerMatcherTest {
    @Test void normalizesCaseSpacesAndPunctuation() {
        assertTrue(AnswerMatcher.matches("  ¡TRONCO   de roble! ", List.of("Tronco de roble"), "es", 0));
    }
    @Test void japanesePreservesVoicingAndLongVowels() {
        assertTrue(AnswerMatcher.matches("ダイヤ モンド。", List.of("ダイヤモンド"), "ja", 0));
        assertFalse(AnswerMatcher.matches("タイヤモント", List.of("ダイヤモンド"), "ja", 0));
        assertNotEquals(AnswerMatcher.normalize("ビール", "ja"), AnswerMatcher.normalize("ビル", "ja"));
    }
    @Test void canonicalCombiningCharactersMatch() {
        assertTrue(AnswerMatcher.matches("タ\u3099イヤモント\u3099", List.of("ダイヤモンド"), "ja", 0));
    }
    @Test void toleranceIsBoundedAndShortWordsAreExact() {
        assertTrue(AnswerMatcher.matches("diamantx", List.of("diamante"), "es", .15));
        assertFalse(AnswerMatcher.matches("diamantx", List.of("diamante"), "es", 0));
        assertFalse(AnswerMatcher.matches("oro", List.of("oso"), "es", .25));
        assertFalse(AnswerMatcher.matches("", List.of("diamante"), "es", .25));
        assertFalse(AnswerMatcher.matches("diamante", List.of("diamante"), "es", Double.NaN));
    }
    @Test void accentsAndGermanUmlautsAreNotDestroyed() {
        assertFalse(AnswerMatcher.matches("hölzer", List.of("holzer"), "de", 0));
        assertFalse(AnswerMatcher.matches("carbon", List.of("carbón"), "es", 0));
    }
}
