package com.ggrgg.createredstonelinkgui.common.network;

import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server: Copy current link frequencies into a preset slot.
 * Mirrors the copy flow from ClipboardValueSettingsHandler (right-click copy with clipboard item).
 */
public record CopyToPresetPayload(BlockPos pos, int presetIndex) implements CustomPacketPayload {

    public static final Type<CopyToPresetPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "copy_to_preset")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CopyToPresetPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, CopyToPresetPayload::pos,
        ByteBufCodecs.INT, CopyToPresetPayload::presetIndex,
        CopyToPresetPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(CopyToPresetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.distanceToSqr(payload.pos.getX(), payload.pos.getY(), payload.pos.getZ()) > 64.0) return;
            FrequencyPresetHelper.copyFromLink(player, payload.pos, payload.presetIndex);
        });
    }
}