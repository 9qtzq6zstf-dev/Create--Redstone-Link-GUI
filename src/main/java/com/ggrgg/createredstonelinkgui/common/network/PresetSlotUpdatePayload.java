package com.ggrgg.createredstonelinkgui.common.network;

import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server: Update a single preset slot in the player's attachment.
 */
public record PresetSlotUpdatePayload(int presetIndex, int slotIndex, ItemStack stack) implements CustomPacketPayload {

    public static final Type<PresetSlotUpdatePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "preset_slot_update")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PresetSlotUpdatePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, PresetSlotUpdatePayload::presetIndex,
        ByteBufCodecs.INT, PresetSlotUpdatePayload::slotIndex,
        ItemStack.OPTIONAL_STREAM_CODEC, PresetSlotUpdatePayload::stack,
        PresetSlotUpdatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(PresetSlotUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            FrequencyPresetData data = FrequencyPresetData.get(player);
            data.setStack(payload.presetIndex, payload.slotIndex, payload.stack);
        });
    }
}