package dev.speakcraft.client;

import net.fabricmc.api.ClientModInitializer;
import com.google.gson.*;
import dev.speakcraft.config.ClientSettings;
import dev.speakcraft.networking.Payloads;
import dev.speakcraft.speech.VoskSpeechService;
import dev.speakcraft.ui.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Path;

public final class SpeakCraftClient implements ClientModInitializer {
    public static SpeakCraftClient INSTANCE;
    public ClientSettings settings;
    public final Path directory = FabricLoader.getInstance().getConfigDir().resolve("speakcraft");
    public final VoskSpeechService speech = new VoskSpeechService();
    public String state = "IDLE", token = "", item = "", name = "", reading = "", message = "", transcript = "";
    public int count, queued, duration = 15;
    public long remaining, visibleUntil;
    public double microphoneLevel;
    public boolean microphoneReady;
    private long lastHeartbeat, lastAudio;
    private KeyMapping configKey;
    private long generation;

    @Override public void onInitializeClient() {
        INSTANCE = this;
        try { settings = ClientSettings.load(directory.resolve("client.json")); }
        catch (IOException e) { settings = new ClientSettings(); message = e.getMessage(); }
        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.speakcraft.config", InputConstants.KEY_F8,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("speakcraft", "settings"))));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ToClient.TYPE, (packet, context) -> receive(packet.json()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> configure("hello"));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            stop(); state = "IDLE"; token = ""; message = "";
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.consumeClick()) {
                openSettings();
            }
            long now = System.nanoTime() / 1_000_000;
            if (microphoneReady && !token.isBlank() && now - lastHeartbeat >= 500 && now - lastAudio < 1500) {
                send("heartbeat", ""); lastHeartbeat = now;
            }
            // No capture while Minecraft is paused or the player is dead. Resume is explicit from F8.
            if (microphoneReady && (client.isPaused() || client.player == null || !client.player.isAlive())) suspend("Captura pausada. Reanudar desde configuración.");
        });
        ChallengeHud hud = new ChallengeHud(this);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("speakcraft", "challenge"), hud);
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            ScreenKeyboardEvents.afterKeyPress(screen).register((current, key) -> {
                if (configKey.matches(key)) openSettings();
            });
            ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, delta) -> {
                if (minecraft.player != null) hud.draw(graphics);
            });
        });
    }
    private void openSettings() {
        var minecraft = Minecraft.getInstance();
        var screen = minecraft.gui.screen();
        if (screen instanceof SettingsScreen) return;
        suspend("Configuración abierta; objetos retenidos.");
        // Container screens close their server menu when removed. Do not reopen that stale menu.
        var parent = screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> ? null : screen;
        minecraft.gui.setScreen(new SettingsScreen(parent, this));
    }
    public void configure(String action) {
        if (!ClientPlayNetworking.canSend(Payloads.ToServer.TYPE)) return;
        var json = new JsonObject(); json.addProperty("action", action);
        json.addProperty("language", settings.language); json.addProperty("duration", settings.durationSeconds);
        json.addProperty("tolerance", settings.tolerance);
        ClientPlayNetworking.send(new Payloads.ToServer(json.toString()));
    }
    public String settingsKeyName() { return configKey.getTranslatedKeyMessage().getString(); }
    public void send(String action, String text) {
        if (!ClientPlayNetworking.canSend(Payloads.ToServer.TYPE)) return;
        var json = new JsonObject(); json.addProperty("action", action); json.addProperty("token", token);
        json.addProperty("text", text.substring(0, Math.min(text.length(), 400)));
        ClientPlayNetworking.send(new Payloads.ToServer(json.toString()));
    }
    public void stop() { generation++; speech.stop(); microphoneReady = false; microphoneLevel = 0; }
    public void suspend(String reason) {
        if (!token.isBlank() && (state.equals("PENDING") || state.equals("LISTENING"))) send("suspend", reason);
        stop();
        if (!token.isBlank()) { state = "CANCELLED"; message = reason; }
    }
    private void receive(String raw) {
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        String next = json.get("state").getAsString();
        settings.language = json.get("language").getAsString();
        if (next.equals("SETTINGS")) return;
        String nextToken = json.get("token").getAsString();
        // The server may suspend a new parcel before requesting audio (for example, missing vocabulary).
        if (!next.equals("PENDING") && !next.equals("CANCELLED") && !nextToken.isBlank() && !nextToken.equals(token)) return;
        state = next; message = json.get("message").getAsString();
        remaining = json.get("remaining").getAsLong(); duration = json.get("duration").getAsInt();
        queued = json.get("queued").getAsInt();
        if (json.has("item")) {
            item = json.get("item").getAsString(); name = json.get("name").getAsString();
            count = json.get("count").getAsInt(); reading = json.get("reading").getAsString();
        }
        if (next.equals("PENDING")) {
            stop(); token = nextToken; transcript = ""; microphoneLevel = 0;
            if (Minecraft.getInstance().gui.screen() instanceof SettingsScreen) { suspend("Configuración abierta"); return; }
            startSpeech();
        } else if (next.equals("SUCCESS") || next.equals("FAILED") || next.equals("CANCELLED")) {
            if (next.equals("CANCELLED")) token = nextToken;
            stop(); visibleUntil = System.nanoTime() / 1_000_000 + 2500;
            if (!next.equals("CANCELLED")) {
                var sound = next.equals("SUCCESS") ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.NOTE_BLOCK_BASS.value();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1f, (float) settings.effectsVolume));
                token = "";
            }
        }
    }
    private void startSpeech() {
        final long session = generation;
        speech.start(directory.resolve("models").resolve(settings.language), settings.microphone, new VoskSpeechService.Listener() {
            private void onClient(Runnable task) {
                Minecraft.getInstance().execute(() -> { if (generation == session) task.run(); });
            }
            @Override public void ready() { onClient(() -> {
                microphoneReady = true; lastAudio = System.nanoTime() / 1_000_000; send("ready", "");
            }); }
            @Override public void text(String value, boolean complete) { onClient(() -> {
                if (!value.isBlank()) transcript = value;
                if (complete && !value.isBlank()) send("transcript", value);
            }); }
            @Override public void level(double rms) { onClient(() -> {
                microphoneLevel = rms; lastAudio = System.nanoTime() / 1_000_000;
            }); }
            @Override public void error(String error) { onClient(() -> suspend(error)); }
        });
    }
}
