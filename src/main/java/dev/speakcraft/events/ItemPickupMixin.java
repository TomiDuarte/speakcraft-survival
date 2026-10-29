package dev.speakcraft.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.speakcraft.server.ChallengeServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {
    // Vanilla has already checked pickup delay and owner before this call.
    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean speakcraft$escrow(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        if (ChallengeServer.hold(inventory.player, stack)) return true;
        return original.call(inventory, stack);
    }
}
