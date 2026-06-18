package com.ggrgg.createredstonelinkgui.common.network;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RedstoneLinkFrequencyPayload(BlockPos pos, ItemStack selectedItem, int slotIndex) implements CustomPacketPayload {
    
    public static final Type<RedstoneLinkFrequencyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "link_frequency"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RedstoneLinkFrequencyPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RedstoneLinkFrequencyPayload::pos,
            ItemStack.OPTIONAL_STREAM_CODEC, RedstoneLinkFrequencyPayload::selectedItem,
            ByteBufCodecs.INT, RedstoneLinkFrequencyPayload::slotIndex,
            RedstoneLinkFrequencyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(RedstoneLinkFrequencyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();

            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            var be = level.getBlockEntity(pos);
            if (be != null) {
                LinkBehaviour behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
                if (behaviour != null) {
                    RedstoneLinkMenu.applyFrequencyChangeDirect(behaviour, payload.slotIndex() == 0, payload.selectedItem());
                    be.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                } else {
                    // Try VoidLinkBehaviour (Create Utilities)
                    Object vlb = com.ggrgg.createredstonelinkgui.common.VoidLinkHelper.getBehaviour(level, pos);
                    if (vlb != null) {
                        // Ownership check — non-owners cannot change frequencies
                        if (!com.ggrgg.createredstonelinkgui.common.VoidLinkHelper.canInteract(vlb, player)) return;
                        VoidLinkMenu.applyFrequencyChangeDirect(vlb, payload.slotIndex() == 0, payload.selectedItem());
                        be.setChanged();
                        level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                    }
                }
            }
        });
    }
}