package com.ggrgg.createredstonelinkgui.common.network;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RedstoneLinkModeTogglePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RedstoneLinkModeTogglePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "link_mode_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RedstoneLinkModeTogglePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RedstoneLinkModeTogglePayload::pos,
            RedstoneLinkModeTogglePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(RedstoneLinkModeTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();

            // Verification check: Stop packets sent via malicious clients across distances
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof RedstoneLinkBlock linkBlock) {
                linkBlock.toggleMode(state, level, pos);
                level.scheduleTick(pos, linkBlock, 1);
            }
        });
    }
}
