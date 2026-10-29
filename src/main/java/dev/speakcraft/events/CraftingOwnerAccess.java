package dev.speakcraft.events;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractCraftingMenu.class)
public interface CraftingOwnerAccess {
    @Invoker("owner") Player speakcraft$owner();
}
