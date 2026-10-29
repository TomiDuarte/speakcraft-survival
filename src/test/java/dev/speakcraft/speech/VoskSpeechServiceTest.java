package dev.speakcraft.speech;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class VoskSpeechServiceTest {
    @TempDir Path temp;
    @Test void missingModelReportsErrorWithoutOpeningMicrophone() throws Exception {
        var ready = new AtomicBoolean(); var failure = new CompletableFuture<String>();
        try (var service = new VoskSpeechService()) {
            service.start(temp.resolve("missing"), "", new VoskSpeechService.Listener() {
                public void ready() { ready.set(true); }
                public void text(String text, boolean complete) { fail("No audio should have been recognized"); }
                public void level(double rms) { fail("No audio should have been captured"); }
                public void error(String message) { failure.complete(message); }
            });
            assertTrue(failure.get(5, TimeUnit.SECONDS).contains("Falta el modelo"));
            assertFalse(ready.get());
        }
    }
    @Test void missingSelectedMicrophoneIsATechnicalError() {
        assertThrows(LineUnavailableException.class, () -> VoskSpeechService.microphone("SpeakCraft-nonexistent-device-for-test"));
    }
    @Test @EnabledOnOs(OS.WINDOWS) void bundledWindowsNativeLibraryLoads() {
        // An IOException is the expected Vosk response to an absent model. A DLL/JNA problem
        // would instead throw LinkageError and fail this test. No microphone is opened.
        LibVosk.setLogLevel(LogLevel.WARNINGS);
        assertThrows(IOException.class, () -> new Model(temp.resolve("missing-native-model").toString()));
    }
}
