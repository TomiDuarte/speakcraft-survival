package dev.speakcraft.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.speakcraft.server.ChallengeServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Optional;

@Mixin(AbstractContainerMenu.class)
public abstract class CraftingTransferMixin {
    @Shadow public abstract Slot getSlot(int index);

    @Inject(method = "moveItemStackTo", at = @At("HEAD"), cancellable = true)
    private void speakcraft$shiftCraft(ItemStack stack, int from, int to, boolean reverse, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if ((self instanceof InventoryMenu || self instanceof CraftingMenu)
            && getSlot(0).getItem() == stack
            && ChallengeServer.hold(((CraftingOwnerAccess) self).speakcraft$owner(), stack)) cir.setReturnValue(true);
    }

    @WrapOperation(method = "doClick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/Slot;tryRemove(IILnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"))
    private Optional<ItemStack> speakcraft$normalCraft(Slot slot, int count, int maximum, Player player,
                                                     Operation<Optional<ItemStack>> original) {
        Optional<ItemStack> taken = original.call(slot, count, maximum, player);
        if (slot instanceof ResultSlot && taken.isPresent() && ChallengeServer.needsChallenge(player, taken.get())) {
            // The vanilla lambda would update the cursor and then call onTake. Consume the recipe here
            // and suppress just that lambda, so pre-existing cursor objects are never touched.
            ItemStack stack = taken.get(); slot.onTake(player, stack);
            ChallengeServer.hold(player, stack); return Optional.empty();
        }
        return taken;
    }

    @WrapOperation(method = "doClick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/Slot;safeTake(IILnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack speakcraft$throwCraft(Slot slot, int count, int maximum, Player player, Operation<ItemStack> original) {
        ItemStack taken = original.call(slot, count, maximum, player);
        if (slot instanceof ResultSlot && ChallengeServer.hold(player, taken)) return ItemStack.EMPTY;
        return taken;
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void speakcraft$guardHotbarSwap(int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
        Object self = this;
        // Number-key swaps use a separate transfer path. For locked results require click/shift-click/Q.
        if ((self instanceof InventoryMenu || self instanceof CraftingMenu) && slot == 0
            && input == ContainerInput.SWAP && ChallengeServer.needsChallenge(player, getSlot(0).getItem())) {
            ci.cancel(); player.containerMenu.sendAllDataToRemote();
        }
    }
}
