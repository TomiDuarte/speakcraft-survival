package dev.speakcraft.ui;

import dev.speakcraft.client.SpeakCraftClient;
import dev.speakcraft.speech.VoskSpeechService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.util.*;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final SpeakCraftClient client;
    private final VoskSpeechService tester = new VoskSpeechService();
    private final List<String> microphones = new ArrayList<>();
    private String testStatus = "";
    private boolean testing;
    private long testEnds;
    private int top;
    public SettingsScreen(Screen parent, SpeakCraftClient client) {
        super(Component.translatable("speakcraft.settings")); this.parent = parent; this.client = client;
        microphones.add(""); microphones.addAll(VoskSpeechService.microphones());
    }
    private Component label(String key, Object... args) { return Component.translatable("speakcraft." + key, args); }
    private void button(String key, Object value, int x, int y, int w, Runnable action) {
        addRenderableWidget(Button.builder(label(key, value), b -> { stopTest(); action.run(); rebuildWidgets(); }).bounds(x, y, w, 20).build());
    }
    @Override protected void init() {
        int w = Math.min(360, width - 20), left = (width - w) / 2, half = (w - 6) / 2;
        top = Math.max(28, (height - 186) / 2);
        var s = client.settings;
        button("language", s.language, left, top, half, () -> {
            var langs = List.of("es", "de", "ja"); s.language = langs.get((langs.indexOf(s.language) + 1) % langs.size());
        });
        button("duration", s.durationSeconds, left + half + 6, top, half, () -> s.durationSeconds = s.durationSeconds >= 60 ? 5 : s.durationSeconds + 5);
        String mic = s.microphone.isBlank() ? label("default").getString() : s.microphone;
        button("microphone", font.plainSubstrByWidth(mic, w - 90), left, top + 24, w, () -> {
            int index = microphones.indexOf(s.microphone); s.microphone = microphones.get((index + 1) % microphones.size());
        });
        button("tolerance", Math.round(s.tolerance * 100) + "%", left, top + 48, half, () -> s.tolerance = s.tolerance >= .249 ? 0 : Math.round((s.tolerance + .05) * 100) / 100.0);
        button("volume", Math.round(s.effectsVolume * 100) + "%", left + half + 6, top + 48, half, () -> s.effectsVolume = s.effectsVolume >= .99 ? 0 : Math.min(1, s.effectsVolume + .2));
        button("help", label(s.pronunciationHelp ? "on" : "off").getString(), left, top + 72, half, () -> s.pronunciationHelp = !s.pronunciationHelp);
        addRenderableWidget(Button.builder(label("reading_guide"), b -> {
            stopTest();
            // Returning creates a fresh settings screen because removed() closes this screen's audio worker.
            minecraft.gui.setScreen(new ReadingGuideScreen(() -> new SettingsScreen(parent, client)));
        }).bounds(left + half + 6, top + 72, half, 20).build());
        addRenderableWidget(Button.builder(label(testing ? "stop_test" : "test"), b -> {
            if (testing) stopTest(); else startTest(); rebuildWidgets();
        }).bounds(left, top + 96, half, 20).build());
        addRenderableWidget(Button.builder(label("resume"), b -> { stopTest(); saveAndClose(); }).bounds(left + half + 6, top + 96, half, 20).build());
        addRenderableWidget(Button.builder(label("save"), b -> saveAndClose()).bounds(left, top + 120, w, 20).build());
    }
    private void stopTest() { testing = false; tester.stop(); }
    private void startTest() {
        testing = true; testEnds = System.nanoTime() + 15_000_000_000L; testStatus = label("preparing").getString();
        var settings = client.settings;
        tester.start(client.directory.resolve("models").resolve(settings.language), settings.microphone, new VoskSpeechService.Listener() {
            @Override public void ready() { minecraft.execute(() -> { if (testing) testStatus = label("speak_now").getString(); }); }
            @Override public void text(String value, boolean complete) { minecraft.execute(() -> { if (testing && !value.isBlank()) testStatus = value; }); }
            @Override public void level(double rms) { }
            @Override public void error(String value) { minecraft.execute(() -> { if (testing) { stopTest(); testStatus = value; rebuildWidgets(); } }); }
        });
    }
    private void saveAndClose() {
        stopTest();
        try {
            client.settings.save(client.directory.resolve("client.json"));
            client.configure("configure"); minecraft.gui.setScreen(parent);
        } catch (IOException error) { testStatus = error.getMessage(); }
    }
    @Override public void onClose() { saveAndClose(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { tester.close(); }
    @Override public void tick() {
        if (testing && System.nanoTime() >= testEnds) { stopTest(); rebuildWidgets(); }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xF0101A23);
        graphics.centeredText(font, title, width / 2, top - 20, 0xFF6ED8EB);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        String status = testStatus.isBlank() ? label("settings_note").getString() : testStatus;
        graphics.textWithWordWrap(font, Component.literal(status), Math.max(10, (width - 360) / 2), top + 148, Math.min(360, width - 20), 0xFFD7DFE8);
    }
}
