package com.ggrgg.createredstonelinkgui.common.network;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;

public record OpenLinkMenuPayload(BlockPos pos) implements CustomPacketPayload {
    
    public static final Type<OpenLinkMenuPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "open_link_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLinkMenuPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenLinkMenuPayload::pos,
            OpenLinkMenuPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(OpenLinkMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();

            // Distance check — same as other payloads
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            var be = level.getBlockEntity(pos);
            if (be == null) return;

            // Check for Create's LinkBehaviour
            LinkBehaviour behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
            if (behaviour != null) {
                be.setChanged();
                level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);

                player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new RedstoneLinkMenu(id, inv, pos),
                    net.minecraft.network.chat.Component.translatable("container.createredstonelinkgui.redstone_link_menu")
                ), buf -> buf.writeBlockPos(pos));
                return;
            }

            // Check for VoidLinkBehaviour (Create Utilities)
            Object vlb = VoidLinkHelper.getBehaviour(level, pos);
            if (vlb != null) {
                // Ownership check — non-owners cannot open the menu
                if (!VoidLinkHelper.canInteract(vlb, player)) return;

                be.setChanged();
                level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);

                player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new VoidLinkMenu(id, inv, pos),
                    net.minecraft.network.chat.Component.translatable("container.createredstonelinkgui.void_link_menu")
                ), buf -> buf.writeBlockPos(pos));
            }
        });
    }
}