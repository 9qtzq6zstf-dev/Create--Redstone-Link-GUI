package com.ggrgg.createredstonelinkgui.common;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "createredstonelinkgui")
public class CommonEventHandler {

    @SubscribeEvent
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();

        // 1. Isolate main hand processing vectors to remove double-click execution bugs
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        // 2. Allow shifting players to continue using Wrenches or clearing items
        if (player.isShiftKeyDown()) return;

        // 3. Check if the block entity has a LinkBehaviour (redstone link frequency system).
        //    This covers vanilla Create redstone links, Aeronautics receivers, and any
        //    other mod's blocks that use the same Create frequency system.
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        LinkBehaviour behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
        if (behaviour == null) return;

        // 4. Only act when clicking directly on a frequency slot (first or second).
        //    If the player missed the slots, let normal block interaction proceed.
        BlockHitResult hitVec = event.getHitVec();
        if (hitVec == null) return;
        Vec3 hitLocation = hitVec.getLocation();
        if (!behaviour.testHit(true, hitLocation) && !behaviour.testHit(false, hitLocation)) return;

        // 5. Only open our menu with empty hand. If the player is holding an item,
        //    let Create handle the slot click (set frequency) normally.
        if (!event.getItemStack().isEmpty()) return;

        // 6. Initiate safe container handling sequences entirely on the logical server
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            be.setChanged();
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);

            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new RedstoneLinkMenu(id, inv, pos),
                Component.literal("Redstone Link Frequency")
            ), buf -> buf.writeBlockPos(pos));
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }

    // == Legacy Aeronautics-specific string check (kept for reference) ==
    //
    // If you need to restrict to specific blocks rather than any LinkBehaviour holder,
    // uncomment the method below and add `|| isAeronauticsReceiverClass(state)` to the
    // condition above, alongside restoring a `BlockState state` variable.
    //
    // private static boolean isAeronauticsReceiverClass(BlockState state) {
    //     String className = state.getBlock().getClass().getName();
    //     return "dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlock".equals(className)
    //         || "dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlock".equals(className);
    // }
}
