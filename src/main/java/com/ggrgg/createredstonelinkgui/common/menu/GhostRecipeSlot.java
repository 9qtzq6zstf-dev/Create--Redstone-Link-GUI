package com.ggrgg.createredstonelinkgui.common.menu;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GhostRecipeSlot extends Slot {

    // Shared dummy container to avoid per-slot allocation of an unused SimpleContainer
    private static final SimpleContainer DUMMY_CONTAINER = new SimpleContainer(1);

    private final int slotIndex;
    private final BiConsumer<Integer, ItemStack> updateCallback;
    private final Supplier<ItemStack> getCallback;

    public GhostRecipeSlot(int slotIndex, int x, int y, Supplier<ItemStack> getCallback, BiConsumer<Integer, ItemStack> updateCallback) {
        super(DUMMY_CONTAINER, 0, x, y);
        this.slotIndex = slotIndex;
        this.getCallback = getCallback;
        this.updateCallback = updateCallback;
    }

    @Override
    public ItemStack getItem() {
        return this.getCallback.get();
    }

    @Override
    public void set(ItemStack stack) {
        if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1); // Standardized down to 1 item to preserve visibility rules
            this.updateCallback.accept(this.slotIndex, copy);
        } else {
            this.updateCallback.accept(this.slotIndex, ItemStack.EMPTY);
        }
    }

    @Override public boolean mayPickup(Player player) { return false; }
    @Override public boolean mayPlace(ItemStack stack) { return false; }
}
