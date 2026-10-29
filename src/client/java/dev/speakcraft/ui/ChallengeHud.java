package dev.speakcraft.ui;

import dev.speakcraft.client.SpeakCraftClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class ChallengeHud implements HudElement {
    private final SpeakCraftClient model;
    public ChallengeHud(SpeakCraftClient model) { this.model = model; }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (Minecraft.getInstance().gui.screen() != null) return;
        draw(graphics);
    }
    public void draw(GuiGraphicsExtractor graphics) {
        if (model.state.equals("IDLE") || Minecraft.getInstance().gui.screen() instanceof SettingsScreen) return;
        if ((model.state.equals("SUCCESS") || model.state.equals("FAILED")) && System.nanoTime() / 1_000_000 > model.visibleUntil) return;
        int w = Math.min(328, graphics.guiWidth() - 16), x = (graphics.guiWidth() - w) / 2, y = 10;
        var font = Minecraft.getInstance().font;
        int nameHeight = Math.max(9, font.split(Component.literal(model.name), w - 44).size() * 9);
        boolean showReading = model.settings.pronunciationHelp && model.settings.language.equals("ja");
        Component hint = !showReading ? Component.literal("×" + model.count)
            : model.reading.isBlank() ? Component.translatable("speakcraft.reading_missing")
            : Component.translatable("speakcraft.reading", model.reading);
        int hintHeight = Math.max(9, font.split(hint, w - 44).size() * 9);
        int body = y + nameHeight - 9 + hintHeight - 9;
        int accent = switch (model.state) {
            case "SUCCESS" -> 0xFF63E6A3;
            case "FAILED" -> 0xFFFF7078;
            case "CANCELLED" -> 0xFFFFCE73;
            default -> 0xFF6ED8EB;
        };
        graphics.fill(x, y, x + w, body + 126, 0xED101A23);
        graphics.fill(x, y, x + w, y + 2, accent);
        graphics.text(font, "SPEAKCRAFT  /  " + model.settings.language.toUpperCase(java.util.Locale.ROOT) + "  ×" + model.count, x + 10, y + 10, accent);
        if (!model.item.isBlank()) graphics.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(model.item))), x + 10, y + 29);
        graphics.textWithWordWrap(font, Component.literal(model.name), x + 34, y + 29, w - 44, 0xFFFFFFFF);
        graphics.textWithWordWrap(font, hint, x + 34, y + 32 + nameHeight, w - 44, 0xFFB0C4D4);
        boolean listening = model.state.equals("LISTENING");
        String label = listening ? String.format(java.util.Locale.ROOT, "%.1f s", model.remaining / 1000.0) : model.state;
        graphics.text(font, label, x + 10, body + 58, accent);
        graphics.text(font, Component.translatable(model.microphoneReady ? "speakcraft.mic_on" : "speakcraft.mic_off"), x + w - 92, body + 58, 0xFFB0C4D4);
        graphics.fill(x + 10, body + 71, x + w - 10, body + 75, 0xFF263847);
        double fraction = listening ? Math.clamp(model.remaining / (model.duration * 1000.0), 0, 1) : 0;
        graphics.fill(x + 10, body + 71, x + 10 + (int) ((w - 20) * fraction), body + 75, accent);
        graphics.fill(x + w - 92, body + 68, x + w - 92 + (int) (Math.min(1, model.microphoneLevel * 8) * 80), body + 70, 0xFF63E6A3);
        graphics.enableScissor(x + 10, body + 80, x + w - 10, body + 109);
        if (model.state.equals("CANCELLED") || model.state.equals("PENDING")) {
            graphics.textWithWordWrap(font, Component.literal(model.message), x + 10, body + 83, w - 20, 0xFFDDDFE6);
        } else {
            String text = model.state.equals("SUCCESS") ? "UNLOCKED!" : model.state.equals("FAILED") ? "DELETED!" : model.transcript;
            graphics.text(font, font.plainSubstrByWidth(text, w - 20), x + 10, body + 85, 0xFFFFFFFF);
        }
        graphics.disableScissor();
        graphics.text(font, font.plainSubstrByWidth(Component.translatable("speakcraft.hud_footer", model.settingsKeyName(), model.queued).getString(), w - 20), x + 10, body + 113, 0xFF91A9BA);
    }
}
