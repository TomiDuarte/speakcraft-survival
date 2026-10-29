package dev.speakcraft.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ClientSettingsTest {
    @TempDir Path temp;
    @Test void allSettingsPersistAcrossReload() throws Exception {
        var path = temp.resolve("client.json"); var settings = new ClientSettings();
        settings.language = "ja"; settings.durationSeconds = 25; settings.microphone = "Selected device";
        settings.tolerance = .2; settings.effectsVolume = .4; settings.pronunciationHelp = false; settings.save(path);
        var restored = ClientSettings.load(path);
        assertEquals("ja", restored.language); assertEquals(25, restored.durationSeconds);
        assertEquals("Selected device", restored.microphone); assertEquals(.2, restored.tolerance);
        assertEquals(.4, restored.effectsVolume); assertFalse(restored.pronunciationHelp);
    }
    @Test void invalidConfigurationIsClamped() {
        var settings = new ClientSettings(); settings.durationSeconds = -5; settings.tolerance = Double.NaN;
        settings.effectsVolume = 99; settings.language = null; settings.validate();
        assertEquals(5, settings.durationSeconds); assertEquals(.1, settings.tolerance);
        assertEquals(1, settings.effectsVolume); assertEquals("es", settings.language);
    }
}
