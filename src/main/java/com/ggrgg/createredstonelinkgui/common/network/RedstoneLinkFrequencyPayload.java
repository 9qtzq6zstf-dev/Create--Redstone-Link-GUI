package com.ggrgg.createredstonelinkgui.common.network;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
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

            // Verification check: Stop packets sent via malicious clients across distances
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            if (level.getBlockEntity(pos) instanceof RedstoneLinkBlockEntity linkBe) {
                LinkBehaviour behaviour = BlockEntityBehaviour.get(linkBe, LinkBehaviour.TYPE);
                if (behaviour != null) {
                    RedstoneLinkMenu.applyFrequencyChangeDirect(behaviour, payload.slotIndex() == 0, payload.selectedItem());
                    
                    linkBe.setChanged();
                    level.sendBlockUpdated(pos, linkBe.getBlockState(), linkBe.getBlockState(), 3);
                }
            }
        });
    }
}
