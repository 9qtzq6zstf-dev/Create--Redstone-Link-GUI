package com.ggrgg.createredstonelinkgui.compat.emi;

import com.ggrgg.createredstonelinkgui.client.screen.VoidLinkConfigScreen;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class VoidLinkEmiDragHandler implements EmiDragDropHandler<VoidLinkConfigScreen> {

    @Override
    public boolean dropStack(VoidLinkConfigScreen screen, EmiIngredient stack, int x, int y) {
        if (stack.isEmpty()) return false;
        var emiStacks = stack.getEmiStacks();
        if (emiStacks.isEmpty()) return false;
        ItemStack itemStack = emiStacks.get(0).getItemStack();
        if (itemStack.isEmpty()) return false;

        Bounds slot1Bounds = toBounds(screen.slot1Bounds);
        Bounds slot2Bounds = toBounds(screen.slot2Bounds);

        if (slot1Bounds.contains(x, y)) { screen.updateFrequencySlot(0, itemStack); return true; }
        else if (slot2Bounds.contains(x, y)) { screen.updateFrequencySlot(1, itemStack); return true; }
        return false;
    }

    @Override
    public void render(VoidLinkConfigScreen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        if (dragged == null || dragged.isEmpty()) return;
        Bounds slot1 = toBounds(screen.slot1Bounds);
        Bounds slot2 = toBounds(screen.slot2Bounds);
        int highlightColor = 0x8822BB33;
        if (slot1.contains(mouseX, mouseY))
            draw.fill(slot1.x(), slot1.y(), slot1.x() + slot1.width(), slot1.y() + slot1.height(), highlightColor);
        else if (slot2.contains(mouseX, mouseY))
            draw.fill(slot2.x(), slot2.y(), slot2.x() + slot2.width(), slot2.y() + slot2.height(), highlightColor);
    }

    private static Bounds toBounds(net.minecraft.client.renderer.Rect2i rect) {
        return new Bounds(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }
}