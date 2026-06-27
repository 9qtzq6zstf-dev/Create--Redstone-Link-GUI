package com.ggrgg.createredstonelinkgui.compat.frequency;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Shared utility for frequency symbol item detection.
 * Eliminates the duplicated isFrequencySymbol method between
 * AbstractLinkConfigScreen and FrequencyPresetPanel.
 */
public class FrequencyItemHelper {

    /**
     * Check if the given ItemStack is a frequency symbol item
     * (from the "frequency" namespace, with a "symbol_" path, excluding "symbol_frame").
     */
    public static boolean isFrequencySymbol(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("frequency")) return false;
        String path = id.getPath();
        if (!path.startsWith("symbol_")) return false;
        if (path.equals("symbol_frame")) return false;
        return true;
    }
}