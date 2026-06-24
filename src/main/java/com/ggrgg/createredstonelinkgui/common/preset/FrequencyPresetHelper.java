package com.ggrgg.createredstonelinkgui.common.preset;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Helper to interact with link behaviours via the ClipboardCloneable interface.
 * Mirrors the pattern used in ClipboardValueSettingsHandler.interact().
 */
public class FrequencyPresetHelper {

    /**
     * Copy frequencies from the link at pos into the preset under the given index.
     * Uses the ClipboardCloneable interface on the behaviour (same as clipboard item copy).
     * Returns true if any data was copied.
     */
    public static boolean copyFromLink(Player player, BlockPos pos, int presetIndex) {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        CompoundTag copiedTag = new CompoundTag();
        boolean anySuccess = false;

        // Try behaviours (LinkBehaviour, VoidLinkBehaviour both implement ClipboardCloneable)
        if (be instanceof net.minecraft.world.level.block.entity.BlockEntity) {
            // Use the same pattern as ClipboardValueSettingsHandler: iterate all behaviours
            if (be instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smartBE) {
                for (BlockEntityBehaviour behaviour : smartBE.getAllBehaviours()) {
                    if (behaviour instanceof ClipboardCloneable cc) {
                        CompoundTag tag = new CompoundTag();
                        boolean success = cc.writeToClipboard(level.registryAccess(), tag, Direction.NORTH);
                        if (success) {
                            copiedTag.put(cc.getClipboardKey(), tag);
                            anySuccess = true;
                        }
                    }
                }
                // Also check if the BE itself is ClipboardCloneable
                if (smartBE instanceof ClipboardCloneable ccbe) {
                    CompoundTag tag = new CompoundTag();
                    boolean success = ccbe.writeToClipboard(level.registryAccess(), tag, Direction.NORTH);
                    if (success) {
                        copiedTag.put(ccbe.getClipboardKey(), tag);
                        anySuccess = true;
                    }
                }
            }
        }

        if (!anySuccess) return false;

        // Store in player attachment
        FrequencyPresetData data = FrequencyPresetData.get(player);
        // Use the tag stored under "Frequencies" key (the clipboard key used by both LinkBehaviour and VoidLinkBehaviour)
        if (copiedTag.contains("Frequencies")) {
            data.setFromTag(presetIndex, copiedTag.getCompound("Frequencies"), level.registryAccess());
        } else {
            // Fallback: use whatever we got
            for (String key : copiedTag.getAllKeys()) {
                data.setFromTag(presetIndex, copiedTag.getCompound(key), level.registryAccess());
                break;
            }
        }
        // Attachment data is automatically saved by NeoForge
        return true;
    }

    /**
     * Paste frequencies from the preset into the link at pos.
     * Uses the ClipboardCloneable interface on the behaviour (same as clipboard item paste).
     * Returns true if the paste succeeded.
     */
    public static boolean pasteToLink(Player player, BlockPos pos, int presetIndex) {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        FrequencyPresetData data = FrequencyPresetData.get(player);
        CompoundTag presetTag = data.getAsTag(presetIndex, level.registryAccess());
        if (presetTag.isEmpty()) return false;

        boolean anySuccess = false;

        // Use the same pattern as ClipboardValueSettingsHandler: iterate all behaviours
        if (be instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smartBE) {
            for (BlockEntityBehaviour behaviour : smartBE.getAllBehaviours()) {
                if (behaviour instanceof ClipboardCloneable cc) {
                    String clipboardKey = cc.getClipboardKey();
                    boolean success = cc.readFromClipboard(
                        level.registryAccess(), presetTag, player, Direction.NORTH, false
                    );
                    anySuccess |= success;
                }
            }
            if (smartBE instanceof ClipboardCloneable ccbe) {
                boolean success = ccbe.readFromClipboard(
                    level.registryAccess(), presetTag, player, Direction.NORTH, false
                );
                anySuccess |= success;
            }
        }

        return anySuccess;
    }
}