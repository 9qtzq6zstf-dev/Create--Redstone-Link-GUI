package com.ggrgg.createredstonelinkgui.common;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

        BlockState state = level.getBlockState(pos);
        boolean isTargetBlock = state.getBlock() instanceof RedstoneLinkBlock
            // Aeronautics: modulating & directional linked receivers (resolved by string to avoid hard dependency)
            || isAeronauticsReceiverClass(state);

        if (isTargetBlock && event.getItemStack().isEmpty()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                
                // 3. Initiate safe container handling sequences entirely on the logical server
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    be.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                    
                    serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new RedstoneLinkMenu(id, inv, pos),
                        Component.literal("Redstone Link Frequency")
                    ), buf -> buf.writeBlockPos(pos));
                }
                
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            }
        }
    }

    /**
     * Checks whether the given block state belongs to one of Aeronautics's linked receiver
     * blocks without importing any Aeronautics classes at compile time.
     * Returns {@code false} if Aeronautics is not installed.
     */
    private static boolean isAeronauticsReceiverClass(BlockState state) {
        // Use the fully-qualified class name string rather than a compile-time import,
        // so Aeronautics remains an optional dependency.
        String className = state.getBlock().getClass().getName();
        return "dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlock".equals(className)
            || "dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlock".equals(className);
    }
}
