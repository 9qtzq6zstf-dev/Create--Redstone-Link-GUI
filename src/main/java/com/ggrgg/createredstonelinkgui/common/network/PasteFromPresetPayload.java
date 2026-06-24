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
 * Client -> Server: Paste preset frequencies into the current link.
 * Mirrors the paste flow from ClipboardValueSettingsHandler (left-click paste with clipboard item).
 */
public record PasteFromPresetPayload(BlockPos pos, int presetIndex) implements CustomPacketPayload {

    public static final Type<PasteFromPresetPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "paste_from_preset")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PasteFromPresetPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, PasteFromPresetPayload::pos,
        ByteBufCodecs.INT, PasteFromPresetPayload::presetIndex,
        PasteFromPresetPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(PasteFromPresetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.distanceToSqr(payload.pos.getX(), payload.pos.getY(), payload.pos.getZ()) > 64.0) return;
            FrequencyPresetHelper.pasteToLink(player, payload.pos, payload.presetIndex);
        });
    }
}