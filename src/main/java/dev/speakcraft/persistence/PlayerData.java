package dev.speakcraft.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.speakcraft.challenge.Ledger;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Stored inside the player's vanilla save, in the same snapshot as their inventory. */
public final class PlayerData {
    private static final Codec<Ledger.Parcel<ItemStack>> PARCEL = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("item").forGetter(Ledger.Parcel::item),
        ItemStack.CODEC.listOf().fieldOf("stacks").forGetter(Ledger.Parcel::stacks)
    ).apply(i, Ledger.Parcel::new));
    public static final Codec<Ledger.Snapshot<ItemStack>> SNAPSHOT = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("language", "es").forGetter(Ledger.Snapshot::language),
        Codec.STRING.listOf().xmap(s -> (Set<String>) new HashSet<>(s), ArrayList::new)
            .fieldOf("unlocked").forGetter(Ledger.Snapshot::unlocked),
        Codec.INT.fieldOf("passed").forGetter(Ledger.Snapshot::passed),
        Codec.INT.fieldOf("failed").forGetter(Ledger.Snapshot::failed),
        PARCEL.listOf().fieldOf("pending").forGetter(Ledger.Snapshot::pending),
        ItemStack.CODEC.listOf().fieldOf("deliveries").forGetter(Ledger.Snapshot::deliveries)
    ).apply(i, Ledger.Snapshot::new));
    public static final AttachmentType<Ledger<ItemStack>> TYPE = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("speakcraft", "progress"), builder -> builder
            .initializer(Ledger::new).persistent(SNAPSHOT.xmap(Ledger::new, Ledger::snapshot)).copyOnDeath());
    private PlayerData() { }
    public static void initialize() { }
}
