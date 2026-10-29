package dev.speakcraft.speech;

import com.google.gson.JsonParser;
import org.vosk.Model;
import org.vosk.Recognizer;
import javax.sound.sampled.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns native Vosk objects and JavaSound on a worker, never the Minecraft render thread. */
public final class VoskSpeechService implements AutoCloseable {
    public interface Listener {
        void ready();
        void text(String value, boolean complete);
        void level(double rms);
        void error(String message);
    }
    private static final AudioFormat FORMAT = new AudioFormat(16000, 16, 1, true, false);
    private static final DataLine.Info LINE_INFO = new DataLine.Info(TargetDataLine.class, FORMAT);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        var thread = new Thread(r, "SpeakCraft-Vosk"); thread.setDaemon(true); return thread;
    });
    private Session active;
    private static final class Session {
        final AtomicBoolean cancelled = new AtomicBoolean();
        volatile TargetDataLine line;
        void stop() {
            cancelled.set(true);
            if (line != null) { line.stop(); line.close(); }
        }
        boolean open(TargetDataLine target) throws LineUnavailableException {
            if (cancelled.get()) return false;
            line = target; target.open(FORMAT, 8192);
            if (cancelled.get()) { target.close(); return false; }
            target.start();
            if (cancelled.get()) { target.stop(); target.close(); return false; }
            return true;
        }
    }
    public static List<String> microphones() {
        var result = new ArrayList<String>();
        for (var info : AudioSystem.getMixerInfo()) {
            if (AudioSystem.getMixer(info).isLineSupported(LINE_INFO)) result.add(deviceId(info));
        }
        return List.copyOf(result);
    }
    private static String deviceId(Mixer.Info info) { return info.getName() + " | " + info.getVendor(); }
    static TargetDataLine microphone(String selected) throws LineUnavailableException {
        if (selected.isBlank()) return (TargetDataLine) AudioSystem.getLine(LINE_INFO);
        for (var info : AudioSystem.getMixerInfo()) {
            if (deviceId(info).equals(selected)) return (TargetDataLine) AudioSystem.getMixer(info).getLine(LINE_INFO);
        }
        throw new LineUnavailableException("El micrófono seleccionado ya no está disponible");
    }
    public synchronized void start(Path modelPath, String device, Listener listener) {
        stop(); Session session = new Session(); active = session;
        worker.execute(() -> {
            try {
                if (!Files.isDirectory(modelPath.resolve("am")) || !Files.isDirectory(modelPath.resolve("conf")))
                    throw new IllegalStateException("Falta el modelo Vosk: " + modelPath);
                if (session.cancelled.get()) return;
                try (Model model = new Model(modelPath.toAbsolutePath().toString());
                     Recognizer recognizer = new Recognizer(model, 16000)) {
                    if (session.cancelled.get()) return;
                    TargetDataLine line = microphone(device);
                    try {
                        if (!session.open(line)) return;
                        listener.ready();
                        byte[] audio = new byte[4096];
                        while (!session.cancelled.get()) {
                            int count = line.read(audio, 0, audio.length);
                            if (session.cancelled.get()) break;
                            if (count <= 0 || !line.isOpen()) throw new IllegalStateException("Se interrumpió la captura del micrófono");
                            double energy = 0;
                            for (int i = 0; i + 1 < count; i += 2) {
                                short sample = (short) ((audio[i] & 255) | (audio[i + 1] << 8));
                                energy += (double) sample * sample;
                            }
                            listener.level(Math.sqrt(energy / (count / 2.0)) / 32768.0);
                            boolean complete = recognizer.acceptWaveForm(audio, count);
                            String json = complete ? recognizer.getResult() : recognizer.getPartialResult();
                            var value = JsonParser.parseString(json).getAsJsonObject().get(complete ? "text" : "partial");
                            if (value != null) listener.text(value.getAsString(), complete);
                        }
                    } finally { line.stop(); line.close(); }
                }
            } catch (Exception | LinkageError problem) {
                if (!session.cancelled.get()) listener.error(problem.getClass().getSimpleName() + ": " + problem.getMessage());
            }
        });
    }
    public synchronized void stop() {
        if (active != null) { active.stop(); active = null; }
    }
    @Override public void close() { stop(); worker.shutdownNow(); }
}
