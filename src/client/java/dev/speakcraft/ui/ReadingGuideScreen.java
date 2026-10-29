package dev.speakcraft.ui;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ReadingGuideScreen extends Screen {
    private final Supplier<Screen> back;
    private boolean spanish;
    private static final String SPANISH = """
        El romaji escribe los sonidos japoneses con letras latinas. Es una ayuda orientativa.

        Vocales como en español. oo, ou y uu indican vocal larga: sostené el sonido.

        sh: como al pedir silencio; ch: como en chico; ts: t+s juntas. La h se aspira, no es muda. La j suena como una y suave, no como la jota española. r: suave, como en caro.

        Ejemplo: 木のツルハシ = ki no tsuruhashi. Las consonantes dobles, como en ingotto, llevan una breve pausa.

        Decí el nombre en japonés. La lectura no es una traducción al español ni una evaluación fonética.
        """;
    public ReadingGuideScreen(Supplier<Screen> back) {
        super(Component.translatable("speakcraft.reading_guide")); this.back = back;
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
            .bounds(width / 2 - 140, height - 28, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal(spanish ? "Idioma del juego" : "Español"), button -> {
            spanish = !spanish; rebuildWidgets();
        }).bounds(width / 2 + 10, height - 28, 130, 20).build());
    }
    @Override public void onClose() { minecraft.gui.setScreen(back.get()); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF101A23);
        graphics.centeredText(font, title, width / 2, 14, 0xFF6ED8EB);
        int w = Math.min(420, width - 24);
        graphics.textWithWordWrap(font, spanish ? Component.literal(SPANISH) : Component.translatable("speakcraft.reading_guide_text"), (width - w) / 2, 36, w, 0xFFD7DFE8);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
