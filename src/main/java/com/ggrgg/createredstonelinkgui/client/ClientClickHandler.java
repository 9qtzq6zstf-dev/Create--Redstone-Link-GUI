package com.ggrgg.createredstonelinkgui.client;

import com.ggrgg.createredstonelinkgui.ClientConfig;
import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.network.OpenLinkMenuPayload;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "createredstonelinkgui", value = Dist.CLIENT)
public class ClientClickHandler {

    @SubscribeEvent
    public static void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        // Only intercept right-click (use item / place block)
        if (!event.isUseItem()) return;
        // If another handler (e.g., move handler) already canceled this, don't interfere
        if (event.isCanceled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.hitResult == null) return;
        if (!(mc.hitResult instanceof BlockHitResult hitVec)) return;

        BlockPos pos = hitVec.getBlockPos();
        Level level = mc.level;
        Player player = mc.player;
        Vec3 hitLocation = hitVec.getLocation();

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        // Check if this block has a link behaviour or void link behaviour
        LinkBehaviour behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
        Object vlb = VoidLinkHelper.getBehaviour(level, pos);

        boolean hasLinkBehaviour = (behaviour != null);
        boolean hasVoidLinkBehaviour = (vlb != null);

        if (!hasLinkBehaviour && !hasVoidLinkBehaviour) return;

        // Read click mode from client config (safe — only runs on client)
        ClientConfig.ClickMode clickMode = ClientConfig.CLICK_MODE.get();
        boolean requiresShift = (clickMode != ClientConfig.ClickMode.SLOT);
        boolean hitAnyBlock = (clickMode == ClientConfig.ClickMode.SHIFT_BLOCK);

        if (requiresShift && !player.isShiftKeyDown()) return;
        if (!requiresShift && player.isShiftKeyDown()) return;

        // Hit-test against the frequency slot(s)
        boolean hitValid = false;

        if (hasLinkBehaviour) {
            if (hitAnyBlock) {
                hitValid = true;
            } else {
                hitValid = behaviour.testHit(true, hitLocation) || behaviour.testHit(false, hitLocation);
            }
        } else if (hasVoidLinkBehaviour) {
            if (hitAnyBlock) {
                hitValid = true;
            } else {
                hitValid = VoidLinkHelper.isHitOnAnySlot(vlb, hitLocation);
            }
        }

        if (!hitValid) return;

        // Check for empty main hand
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHandItem.isEmpty()) return;

        // Cancel the key mapping — prevents the vanilla packet from being sent to the server
        event.setCanceled(true);
        event.setSwingHand(false);

        // Send packet to server to open the menu
        PacketDistributor.sendToServer(new OpenLinkMenuPayload(pos));
    }
}