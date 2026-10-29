package dev.speakcraft;

import com.mojang.serialization.JsonOps;
import dev.speakcraft.challenge.Ledger;
import dev.speakcraft.language.AnswerMatcher;
import dev.speakcraft.persistence.PlayerData;
import dev.speakcraft.server.ChallengeServer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import java.util.List;

/** Loaded only by runGameTest. Neither fake transcripts nor these entry points ship in the mod. */
public final class SurvivalGameTests {
    private ServerPlayer player(GameTestHelper helper) {
        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        // Vanilla's basic mock has no packet listener. Recipe updates require one even offline.
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
            helper.getLevel().getServer(), connection, player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        return player;
    }
    private ItemEntity pickup(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), 0, 2, 0, stack);
        entity.setNoPickUpDelay(); entity.playerTouch(player); return entity;
    }
    private int retained(ServerPlayer player) {
        return ChallengeServer.data(player).snapshot().pending().stream()
            .flatMap(parcel -> parcel.stacks().stream()).mapToInt(ItemStack::getCount).sum();
    }
    private void simulatedVoiceForTestsOnly(ServerPlayer player, String text) {
        var ledger = ChallengeServer.data(player); var token = ledger.begin(); ledger.listen(token, 0, 15);
        ledger.answer(token, AnswerMatcher.matches(text, List.of("Diamante"), "es", 0), 1000);
    }
    @GameTest public void pickupHoldsEntireStack(GameTestHelper helper) {
        var player = player(helper); var entity = pickup(helper, player, new ItemStack(Items.DIAMOND, 32));
        helper.assertTrue(entity.isRemoved(), "Picked entity must be removed exactly once");
        helper.assertTrue(player.getInventory().isEmpty(), "Locked items cannot enter inventory");
        helper.assertValueEqual(retained(player), 32, "Escrow amount"); helper.succeed();
    }
    @GameTest public void successfulVoiceDeliversOnlyOnce(GameTestHelper helper) {
        var player = player(helper); pickup(helper, player, new ItemStack(Items.DIAMOND, 32));
        simulatedVoiceForTestsOnly(player, "Diamante");
        ChallengeServer.flushDeliveries(player, ChallengeServer.data(player));
        ChallengeServer.flushDeliveries(player, ChallengeServer.data(player));
        helper.assertValueEqual(player.getInventory().getItem(0).getCount(), 32, "One delivery only");
        pickup(helper, player, new ItemStack(Items.DIAMOND, 4));
        helper.assertValueEqual(player.getInventory().getItem(0).getCount(), 36, "Unlocked pickup bypasses challenge");
        helper.assertValueEqual(retained(player), 0, "No repeated challenge"); helper.succeed();
    }
    @GameTest public void failurePreservesPreexistingInventory(GameTestHelper helper) {
        var player = player(helper); player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 10));
        pickup(helper, player, new ItemStack(Items.DIAMOND, 4)); var ledger = ChallengeServer.data(player);
        var token = ledger.begin(); ledger.listen(token, 0, 15); ledger.expire(15000);
        helper.assertValueEqual(player.getInventory().getItem(0).getCount(), 10, "Old items must not be destroyed");
        helper.assertValueEqual(retained(player), 0, "Failed acquisition destroyed"); helper.succeed();
    }
    @GameTest public void inventoryCraftRetainsOutputAndConsumesIngredients(GameTestHelper helper) {
        var player = player(helper); var menu = player.inventoryMenu;
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
        helper.assertTrue(menu.getSlot(0).hasItem(), "Recipe should produce planks");
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "Locked result cannot enter cursor");
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "Craft must consume input");
        helper.assertValueEqual(retained(player), 4, "Four planks retained"); helper.succeed();
    }
    @GameTest public void inventoryShiftCraftRetainsAllBatches(GameTestHelper helper) {
        var player = player(helper); var menu = player.inventoryMenu;
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG, 16));
        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertValueEqual(retained(player), 64, "Shift-click crafts all sixteen batches");
        helper.assertTrue(player.getInventory().isEmpty(), "Shift-crafted output must not leak into inventory");
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "All ingredients consumed"); helper.succeed();
    }
    @GameTest public void craftingTableShiftClick(GameTestHelper helper) {
        var player = player(helper);
        var menu = new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO)));
        player.containerMenu = menu; menu.getSlot(1).set(new ItemStack(Items.OAK_LOG, 16));
        helper.assertTrue(menu.getSlot(0).hasItem(), "Table recipe should produce planks");
        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertValueEqual(retained(player), 64, "Table shift-click retains all output");
        helper.assertTrue(player.getInventory().isEmpty(), "No table output can bypass escrow"); helper.succeed();
    }
    @GameTest public void existingCursorIsNotConfiscated(GameTestHelper helper) {
        var player = player(helper); var menu = player.inventoryMenu;
        menu.setCarried(new ItemStack(Items.OAK_PLANKS, 10)); menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertValueEqual(menu.getCarried().getCount(), 10, "Existing cursor amount stays unchanged");
        helper.assertValueEqual(retained(player), 4, "Only the new craft is retained"); helper.succeed();
    }
    @GameTest public void throwingCraftOutputStillRetainsIt(GameTestHelper helper) {
        var player = player(helper); var menu = player.inventoryMenu;
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG)); menu.clicked(0, 1, ContainerInput.THROW, player);
        helper.assertValueEqual(retained(player), 4, "Q cannot throw a locked craft past escrow");
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "Thrown craft consumes its ingredients once");
        helper.assertTrue(player.getInventory().isEmpty(), "No locked output in inventory"); helper.succeed();
    }
    @GameTest public void rightClickCraftKeepsFullRecipeOutput(GameTestHelper helper) {
        var player = player(helper); var menu = player.inventoryMenu;
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG)); menu.clicked(0, 1, ContainerInput.PICKUP, player);
        helper.assertValueEqual(retained(player), 4, "Right click retains the full recipe output");
        helper.assertTrue(menu.getCarried().isEmpty(), "No locked output on cursor"); helper.succeed();
    }
    @GameTest public void hotbarSwapCannotBypassChallenge(GameTestHelper helper) {
        var player = player(helper); var menu = player.inventoryMenu;
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG)); menu.clicked(0, 0, ContainerInput.SWAP, player);
        helper.assertTrue(player.getInventory().isEmpty(), "Locked result cannot enter hotbar");
        helper.assertValueEqual(menu.getSlot(1).getItem().getCount(), 1, "Unsupported swap does not consume input"); helper.succeed();
    }
    @GameTest public void creativeBypassesChallenges(GameTestHelper helper) {
        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        pickup(helper, player, new ItemStack(Items.DIAMOND, 4));
        helper.assertValueEqual(retained(player), 0, "No creative challenges"); helper.succeed();
    }
    @GameTest public void actualStackCodecPersistsEscrowAndUnlocks(GameTestHelper helper) {
        var player = player(helper); pickup(helper, player, new ItemStack(Items.DIAMOND, 4));
        simulatedVoiceForTestsOnly(player, "Diamante");
        pickup(helper, player, new ItemStack(Items.IRON_INGOT, 7));
        var ledger = ChallengeServer.data(player); ledger.language("ja");
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var encoded = PlayerData.SNAPSHOT.encodeStart(ops, ledger.snapshot()).getOrThrow();
        var restored = new Ledger<>(PlayerData.SNAPSHOT.parse(ops, encoded).getOrThrow());
        helper.assertTrue(restored.unlocked("minecraft:diamond"), "Unlock persists");
        helper.assertValueEqual(restored.language(), "ja", "Selected language persists");
        helper.assertValueEqual(restored.current().stacks().getFirst().getCount(), 7, "Escrow stack persists");
        helper.assertValueEqual(restored.deliveries().getFirst().getCount(), 4, "Undelivered success persists"); helper.succeed();
    }
    @GameTest public void fullInventoryKeepsDeliveryInEscrow(GameTestHelper helper) {
        var player = player(helper);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        pickup(helper, player, new ItemStack(Items.DIAMOND, 4)); simulatedVoiceForTestsOnly(player, "Diamante");
        var ledger = ChallengeServer.data(player); ChallengeServer.flushDeliveries(player, ledger);
        helper.assertValueEqual(ledger.deliveries().size(), 1, "Full inventory retains delivery");
        player.getInventory().setItem(0, ItemStack.EMPTY); ChallengeServer.flushDeliveries(player, ledger);
        helper.assertValueEqual(player.getInventory().getItem(0).getCount(), 4, "Delivery after freeing slot");
        helper.assertTrue(ledger.deliveries().isEmpty(), "Delivery consumed once"); helper.succeed();
    }
    @GameTest public void vanillaPlayerSaveRestoresInventoryAndPendingChallenge(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, new ItemStack(Items.APPLE, 3));
        pickup(helper, player, new ItemStack(Items.DIAMOND, 9));
        var ledger = ChallengeServer.data(player); ledger.language("de");
        var oldToken = ledger.begin(); ledger.listen(oldToken, 0, 15);
        var lookup = helper.getLevel().registryAccess();
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, lookup);
        player.saveWithoutId(output);
        var restoredPlayer = player(helper);
        restoredPlayer.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, lookup, output.buildResult()));
        var restored = ChallengeServer.data(restoredPlayer);
        helper.assertValueEqual(retained(restoredPlayer), 9, "Fabric attachment survives vanilla player save/load");
        helper.assertValueEqual(restoredPlayer.getInventory().getItem(0).getCount(), 3, "Inventory restored in same snapshot");
        helper.assertValueEqual(restored.language(), "de", "World-specific language restored");
        helper.assertFalse(restored.valid(oldToken), "Old voice token invalid after reload");
        helper.assertFalse(restored.expire(999999), "Reload cannot delete items before microphone handshake");
        helper.succeed();
    }
}
