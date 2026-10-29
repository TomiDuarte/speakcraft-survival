package dev.speakcraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;

public final class ClientSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public String language = "es";
    public int durationSeconds = 15;
    public String microphone = "";
    public double tolerance = .10;
    public double effectsVolume = .6;
    public boolean pronunciationHelp = true;
    public void validate() {
        if (!Set.of("es", "de", "ja").contains(language == null ? "" : language)) language = "es";
        durationSeconds = Math.clamp(durationSeconds, 5, 60);
        microphone = microphone == null ? "" : microphone;
        tolerance = Double.isFinite(tolerance) ? Math.clamp(tolerance, 0, .25) : .10;
        effectsVolume = Double.isFinite(effectsVolume) ? Math.clamp(effectsVolume, 0, 1) : .6;
    }
    public static ClientSettings load(Path path) throws IOException {
        if (!Files.exists(path)) return new ClientSettings();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ClientSettings settings = GSON.fromJson(reader, ClientSettings.class);
            if (settings == null) throw new IOException("Empty settings");
            settings.validate(); return settings;
        } catch (RuntimeException e) { throw new IOException("Invalid settings: " + path, e); }
    }
    public void save(Path path) throws IOException {
        validate(); Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(this), StandardCharsets.UTF_8);
        try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
    }
}
