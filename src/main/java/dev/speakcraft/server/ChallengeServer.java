package dev.speakcraft.server;

import com.google.gson.*;
import dev.speakcraft.challenge.*;
import dev.speakcraft.core.SpeakCraft;
import dev.speakcraft.language.*;
import dev.speakcraft.networking.Payloads;
import dev.speakcraft.persistence.PlayerData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** All entry points are Fabric server-thread events or synchronous vanilla inventory hooks. */
public final class ChallengeServer {
    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();
    private static long clock;
    private static final class RuntimeState {
        int duration = 15;
        double tolerance = .10;
        long heartbeat, nextChallenge, lastUpdate, nextDelivery;
        boolean connected;
    }
    private ChallengeServer() { }
    public static Ledger<ItemStack> data(ServerPlayer player) { return player.getAttachedOrCreate(PlayerData.TYPE); }
    public static boolean eligible(Player player) {
        return player instanceof ServerPlayer && !player.isCreative() && !player.isSpectator();
    }
    public static boolean needsChallenge(Player player, ItemStack stack) {
        return eligible(player) && !stack.isEmpty() && !data((ServerPlayer) player).unlocked(id(stack));
    }
    public static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    /** Atomically transfers ownership of this stack to escrow; the caller must not deliver it. */
    public static boolean hold(Player player, ItemStack stack) {
        if (!needsChallenge(player, stack)) return false;
        if (data((ServerPlayer) player).hold(id(stack), stack.copy())) {
            stack.setCount(0); return true;
        }
        return false;
    }
    public static void initialize() {
        PlayerData.initialize();
        ServerPlayNetworking.registerGlobalReceiver(Payloads.ToServer.TYPE, (packet, ctx) -> receive(ctx.player(), packet.json()));
        ServerTickEvents.END_SERVER_TICK.register(ChallengeServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            data(handler.player).pause(); RUNTIME.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { RUNTIME.clear(); clock = 0; });
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> dispatcher.register(
            Commands.literal("speakcraft").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("progress").executes(ctx -> {
                    var saved = data(ctx.getSource().getPlayerOrException()).snapshot();
                    ctx.getSource().sendSuccess(() -> Component.literal("SpeakCraft: " + saved.unlocked().size()
                        + " unlocked; " + saved.passed() + " passed; " + saved.failed() + " failed; "
                        + saved.pending().size() + " pending; " + saved.language()), false); return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    data(player).reset(); status(player, "CANCELLED", "Progreso reiniciado. Objetos retenidos conservados.", null);
                    return 1;
                }))));
    }
    private static void receive(ServerPlayer player, String raw) {
        try {
            JsonObject packet = JsonParser.parseString(raw).getAsJsonObject();
            String action = packet.get("action").getAsString();
            boolean firstConnection = player.getAttached(PlayerData.TYPE) == null;
            Ledger<ItemStack> ledger = data(player);
            RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
            if (action.equals("hello") || action.equals("configure")) {
                if (action.equals("configure")) ledger.pause();
                String language = packet.get("language").getAsString();
                if (action.equals("configure") || firstConnection) ledger.language(language);
                runtime.duration = Math.clamp(packet.get("duration").getAsInt(), 5, 60);
                double tolerance = packet.get("tolerance").getAsDouble();
                runtime.tolerance = Double.isFinite(tolerance) ? Math.clamp(tolerance, 0, .25) : .10;
                runtime.connected = true; ledger.resume();
                status(player, "SETTINGS", "", null); return;
            }
            if (action.equals("resume")) { ledger.resume(); runtime.nextChallenge = clock; return; }
            UUID token = UUID.fromString(packet.get("token").getAsString());
            if (!ledger.valid(token)) return;
            switch (action) {
                case "ready" -> {
                    if (ledger.listen(token, clock, runtime.duration)) {
                        runtime.heartbeat = clock; status(player, "LISTENING", "", token);
                    }
                }
                case "heartbeat" -> runtime.heartbeat = clock;
                case "suspend" -> {
                    ledger.suspend(token);
                    String error = packet.has("text") ? packet.get("text").getAsString() : "Captura suspendida";
                    status(player, "CANCELLED", error.substring(0, Math.min(error.length(), 400)), token);
                }
                case "transcript" -> {
                    String text = packet.get("text").getAsString();
                    if (text.length() > 256 || ledger.current() == null) return;
                    ItemStack stack = ledger.current().stacks().getFirst();
                    boolean match = AnswerMatcher.matches(text, ItemNames.answers(stack.getItem().getDescriptionId(), ledger.language()), ledger.language(), runtime.tolerance);
                    if (ledger.answer(token, match, clock)) {
                        status(player, "SUCCESS", "UNLOCKED!", token); runtime.nextChallenge = clock + 2000;
                    }
                }
                default -> { }
            }
        } catch (RuntimeException malformed) {
            SpeakCraft.LOGGER.warn("Rejected malformed SpeakCraft request from {}", player.getUUID());
        }
    }
    private static void tick(MinecraftServer server) {
        clock += 50; // Game time: an integrated-server pause never spends the countdown.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var ledger = player.getAttached(PlayerData.TYPE);
            if (ledger == null) continue;
            RuntimeState runtime = RUNTIME.get(player.getUUID());
            if (!eligible(player) || !player.isAlive()) {
                if (ledger.token() != null) {
                    UUID old = ledger.token(); ledger.pause(); status(player, "CANCELLED", "Desafío pausado: muerte o cambio de modo.", old);
                }
                continue;
            }
            if (!ledger.deliveries().isEmpty() && (runtime == null || clock >= runtime.nextDelivery)) {
                flushDeliveries(player, ledger);
                if (runtime != null) runtime.nextDelivery = clock + 1000;
            }
            if (runtime == null || !runtime.connected || !ServerPlayNetworking.canSend(player, Payloads.ToClient.TYPE)) continue;
            if (ledger.token() != null && ledger.state() == ChallengeState.LISTENING) {
                UUID token = ledger.token();
                if (clock - runtime.heartbeat > 3000) {
                    ledger.suspend(token); status(player, "CANCELLED", "Se interrumpió el audio. Los objetos siguen retenidos.", token);
                } else if (ledger.expire(clock)) {
                    status(player, "FAILED", "DELETED!", token); runtime.nextChallenge = clock + 2000;
                } else if (clock - runtime.lastUpdate >= 250) {
                    status(player, "LISTENING", "", token); runtime.lastUpdate = clock;
                }
            }
            if (ledger.token() == null && clock >= runtime.nextChallenge) {
                UUID token = ledger.begin();
                if (token != null) {
                    ItemStack stack = ledger.current().stacks().getFirst();
                    if (ItemNames.answers(stack.getItem().getDescriptionId(), ledger.language()).isEmpty()) {
                        ledger.suspend(token); status(player, "CANCELLED", "Este objeto no tiene nombre oficial en el idioma seleccionado.", token);
                    } else status(player, "PENDING", "Preparando micrófono y modelo…", token);
                }
            }
        }
    }
    public static void flushDeliveries(ServerPlayer player, Ledger<ItemStack> ledger) {
        // Inventory.add mutates only the owned delivery stack. A full inventory leaves it in the save.
        while (!ledger.deliveries().isEmpty()) {
            ItemStack stack = ledger.deliveries().getFirst();
            int before = stack.getCount(); player.getInventory().add(stack);
            if (stack.isEmpty()) ledger.deliveredFirst();
            if (stack.getCount() != before) player.containerMenu.broadcastChanges();
            if (!stack.isEmpty()) break;
        }
    }
    private static void status(ServerPlayer player, String state, String message, UUID token) {
        if (!ServerPlayNetworking.canSend(player, Payloads.ToClient.TYPE)) return;
        var ledger = data(player); var json = new JsonObject();
        json.addProperty("state", state); json.addProperty("message", message);
        json.addProperty("token", token == null ? "" : token.toString());
        json.addProperty("language", ledger.language()); json.addProperty("remaining", ledger.remaining(clock));
        json.addProperty("queued", ledger.queued());
        var runtime = RUNTIME.get(player.getUUID()); json.addProperty("duration", runtime == null ? 15 : runtime.duration);
        if (ledger.current() != null && (state.equals("PENDING") || state.equals("LISTENING") || state.equals("CANCELLED"))) {
            var parcel = ledger.current(); var stack = parcel.stacks().getFirst();
            json.addProperty("item", parcel.item());
            json.addProperty("count", parcel.stacks().stream().mapToInt(ItemStack::getCount).sum());
            json.addProperty("name", ItemNames.name(stack.getItem().getDescriptionId(), ledger.language()).orElse(parcel.item()));
            json.addProperty("reading", ItemNames.reading(stack.getItem().getDescriptionId(), ledger.language()));
        }
        ServerPlayNetworking.send(player, new Payloads.ToClient(json.toString()));
    }
}
