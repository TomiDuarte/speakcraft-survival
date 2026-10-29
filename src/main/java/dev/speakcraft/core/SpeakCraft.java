package dev.speakcraft.core;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SpeakCraft implements ModInitializer {
    public static final String ID = "speakcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    @Override public void onInitialize() {
        LOGGER.info("SpeakCraft Survival initializing");
        dev.speakcraft.networking.Payloads.register();
        dev.speakcraft.server.ChallengeServer.initialize();
    }
}
