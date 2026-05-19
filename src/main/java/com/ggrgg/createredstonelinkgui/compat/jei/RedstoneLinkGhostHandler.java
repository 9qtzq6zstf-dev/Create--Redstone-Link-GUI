package com.ggrgg.createredstonelinkgui.compat.jei;

import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedstoneLinkGhostHandler implements IGhostIngredientHandler<RedstoneLinkConfigScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(RedstoneLinkConfigScreen screen, ITypedIngredient<I> typedIngredient, boolean doStart) {
        var itemStackOptional = typedIngredient.getIngredient(VanillaTypes.ITEM_STACK);
        
        // Fast-fail check: Ignore Fluid/Gas/Energy tokens cleanly to prevent garbage heap allocation
        if (itemStackOptional.isEmpty()) {
            return Collections.emptyList();
        }

        ItemStack stack = itemStackOptional.get();

        // Optimized Allocation: Explicitly size the list to 2 elements to prevent drag rendering arrays resizes
        List<Target<I>> targets = new ArrayList<>(2);

        targets.add(new Target<>() {
            @Override public Rect2i getArea() { return screen.slot1Bounds; }
            @Override public void accept(I ing) { screen.updateFrequencySlot(0, stack); }
        });

        targets.add(new Target<>() {
            @Override public Rect2i getArea() { return screen.slot2Bounds; }
            @Override public void accept(I ing) { screen.updateFrequencySlot(1, stack); }
        });

        return targets;
    }

    @Override public void onComplete() {}
}
