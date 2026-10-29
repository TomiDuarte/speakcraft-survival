package dev.speakcraft.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class Payloads {
    private Payloads() { }
    public record ToServer(String json) implements CustomPacketPayload {
        public static final Type<ToServer> TYPE = new Type<>(Identifier.fromNamespaceAndPath("speakcraft", "request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToServer> CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeUtf(value.json(), 2048), buffer -> new ToServer(buffer.readUtf(2048)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record ToClient(String json) implements CustomPacketPayload {
        public static final Type<ToClient> TYPE = new Type<>(Identifier.fromNamespaceAndPath("speakcraft", "status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToClient> CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeUtf(value.json(), 8192), buffer -> new ToClient(buffer.readUtf(8192)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ToServer.TYPE, ToServer.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ToClient.TYPE, ToClient.CODEC);
    }
}
