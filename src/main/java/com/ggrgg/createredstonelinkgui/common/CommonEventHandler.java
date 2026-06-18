package com.ggrgg.createredstonelinkgui.common;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;
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

        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (player.isShiftKeyDown()) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        BlockHitResult hitVec = event.getHitVec();
        if (hitVec == null) return;
        Vec3 hitLocation = hitVec.getLocation();

        boolean hitFrequencySlot = false;
        boolean isVoidLink = false;

        // Check for Create's LinkBehaviour
        LinkBehaviour behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
        if (behaviour != null) {
            hitFrequencySlot = behaviour.testHit(true, hitLocation) || behaviour.testHit(false, hitLocation);
        } else {
            // Check for Create Utilities' VoidLinkBehaviour — accept all slots (0, 1, 2)
            Object vlb = VoidLinkHelper.getBehaviour(level, pos);
            if (vlb != null) {
                // Check ownership before allowing menu
                if (!VoidLinkHelper.canInteract(vlb, player)) return;
                hitFrequencySlot = VoidLinkHelper.isHitOnAnySlot(vlb, hitLocation);
                isVoidLink = true;
            }
        }

        if (!hitFrequencySlot) return;
        if (!event.getItemStack().isEmpty()) return;

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            be.setChanged();
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);

            if (isVoidLink) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new VoidLinkMenu(id, inv, pos),
                    Component.translatable("container.createredstonelinkgui.void_link_menu")
                ), buf -> buf.writeBlockPos(pos));
            } else {
                serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new RedstoneLinkMenu(id, inv, pos),
                    Component.translatable("container.createredstonelinkgui.redstone_link_menu")
                ), buf -> buf.writeBlockPos(pos));
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
