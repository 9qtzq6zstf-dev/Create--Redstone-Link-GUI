package com.ggrgg.createredstonelinkgui.common.preset;

import java.util.ArrayList;
import java.util.List;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Stores 4 frequency presets for the copy/paste panel.
 * Each preset holds two ItemStacks (frequency 0 and frequency 1).
 */
public class FrequencyPresetData implements INBTSerializable<CompoundTag> {

    public static final int PRESET_COUNT = 8;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreateRedstoneLinkGUI.MODID);

    public static final Supplier<AttachmentType<FrequencyPresetData>> ATTACHMENT =
        ATTACHMENT_TYPES.register(
            "frequency_presets",
            () -> AttachmentType.builder(FrequencyPresetData::new)
                .serialize(FrequencyPresetData.SERIALIZER)
                .copyOnDeath()
                .build()
        );

    private static final IAttachmentSerializer<CompoundTag, FrequencyPresetData> SERIALIZER =
        new IAttachmentSerializer<CompoundTag, FrequencyPresetData>() {
            @Override
            public CompoundTag write(FrequencyPresetData attachment, HolderLookup.Provider provider) {
                return attachment.serializeNBT(provider);
            }

            @Override
            public FrequencyPresetData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                FrequencyPresetData data = new FrequencyPresetData();
                data.deserializeNBT(provider, tag);
                return data;
            }
        };

    private final List<Preset> presets;

    public FrequencyPresetData() {
        this.presets = new ArrayList<>(PRESET_COUNT);
        for (int i = 0; i < PRESET_COUNT; i++) {
            presets.add(new Preset());
        }
    }

    public static FrequencyPresetData get(Player player) {
        return player.getData(ATTACHMENT);
    }

    public ItemStack getStack(int presetIndex, int slotIndex) {
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return ItemStack.EMPTY;
        return slotIndex == 0 ? presets.get(presetIndex).stack0 : presets.get(presetIndex).stack1;
    }

    public void setStack(int presetIndex, int slotIndex, ItemStack stack) {
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        if (slotIndex == 0) {
            presets.get(presetIndex).stack0 = copy;
        } else {
            presets.get(presetIndex).stack1 = copy;
        }
    }

    /**
     * Set both stacks for a preset from a CompoundTag (as produced by ClipboardCloneable.writeToClipboard).
     */
    public void setFromTag(int presetIndex, CompoundTag tag, HolderLookup.Provider registries) {
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return;
        Preset p = presets.get(presetIndex);
        p.stack0 = tag.contains("First", Tag.TAG_COMPOUND)
            ? ItemStack.parseOptional(registries, tag.getCompound("First"))
            : ItemStack.EMPTY;
        p.stack1 = tag.contains("Last", Tag.TAG_COMPOUND)
            ? ItemStack.parseOptional(registries, tag.getCompound("Last"))
            : ItemStack.EMPTY;
    }

    /**
     * Write both stacks of a preset into a CompoundTag (matching ClipboardCloneable format).
     */
    public CompoundTag getAsTag(int presetIndex, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return tag;
        Preset p = presets.get(presetIndex);
        // Always write both "First" and "Last" keys, even if empty.
        // LinkBehaviour.readFromClipboard requires both tags to exist.
        tag.put("First", p.stack0.saveOptional(registries));
        tag.put("Last", p.stack1.saveOptional(registries));
        return tag;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < PRESET_COUNT; i++) {
            CompoundTag entry = new CompoundTag();
            Preset p = presets.get(i);
            if (!p.stack0.isEmpty()) entry.put("0", p.stack0.save(registries));
            if (!p.stack1.isEmpty()) entry.put("1", p.stack1.save(registries));
            list.add(entry);
        }
        root.put("presets", list);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag root) {
        ListTag list = root.getList("presets", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), PRESET_COUNT); i++) {
            CompoundTag entry = list.getCompound(i);
            Preset p = presets.get(i);
            p.stack0 = entry.contains("0", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, entry.getCompound("0"))
                : ItemStack.EMPTY;
            p.stack1 = entry.contains("1", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, entry.getCompound("1"))
                : ItemStack.EMPTY;
        }
    }

    // --- Synchronization ---

    /**
     * Serialize presets as a list of CompoundTags for network sync.
     */
    public List<CompoundTag> toTagList(HolderLookup.Provider registries) {
        List<CompoundTag> list = new ArrayList<>(PRESET_COUNT);
        for (int i = 0; i < PRESET_COUNT; i++) {
            list.add(getAsTag(i, registries));
        }
        return list;
    }

    /**
     * Restore presets from a list of CompoundTags received from the server.
     */
    public void fromTagList(HolderLookup.Provider registries, List<CompoundTag> list) {
        for (int i = 0; i < Math.min(list.size(), PRESET_COUNT); i++) {
            setFromTag(i, list.get(i), registries);
        }
    }

    private static class Preset {
        ItemStack stack0 = ItemStack.EMPTY;
        ItemStack stack1 = ItemStack.EMPTY;
    }
}