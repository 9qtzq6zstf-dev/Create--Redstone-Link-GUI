package com.ggrgg.createredstonelinkgui.common.network;

import com.mojang.authlib.GameProfile;
import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VoidLinkClaimPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<VoidLinkClaimPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "void_link_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidLinkClaimPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, VoidLinkClaimPayload::pos,
            VoidLinkClaimPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(VoidLinkClaimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();

            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            Object behaviour = VoidLinkHelper.getBehaviour(level, pos);
            if (behaviour == null) return;

            GameProfile currentOwner = VoidLinkHelper.getOwner(behaviour);
            if (currentOwner == null) {
                VoidLinkHelper.setOwner(behaviour, player.getGameProfile());
            } else if (currentOwner.getId().equals(player.getUUID())) {
                VoidLinkHelper.setOwner(behaviour, null);
            }
        });
    }
}