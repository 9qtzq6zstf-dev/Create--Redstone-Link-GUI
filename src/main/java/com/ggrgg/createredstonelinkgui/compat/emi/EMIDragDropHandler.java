package com.ggrgg.createredstonelinkgui.compat.emi;

import com.ggrgg.createredstonelinkgui.client.screen.AbstractLinkConfigScreen;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

/**
 * Generic EMI drag-drop handler for both RedstoneLinkConfigScreen and VoidLinkConfigScreen.
 * Handles dragging items from EMI into the frequency slots.
 */
public class EMIDragDropHandler<T extends AbstractLinkConfigScreen<?>> implements EmiDragDropHandler<T> {

    @Override
    public boolean dropStack(T screen, EmiIngredient stack, int x, int y) {
        if (stack.isEmpty()) return false;

        var emiStacks = stack.getEmiStacks();
        if (emiStacks.isEmpty()) return false;

        ItemStack itemStack = emiStacks.get(0).getItemStack();
        if (itemStack.isEmpty()) return false;

        // Check frequency slots
        Bounds slot1Bounds = toBounds(screen.slot1Bounds);
        Bounds slot2Bounds = toBounds(screen.slot2Bounds);
        if (slot1Bounds.contains(x, y)) {
            screen.updateFrequencySlot(0, itemStack);
            return true;
        } else if (slot2Bounds.contains(x, y)) {
            screen.updateFrequencySlot(1, itemStack);
            return true;
        }

        // Check preset slots
        if (screen.presetPanel != null) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 2; col++) {
                    Rect2i bounds = screen.presetPanel.getSlotBounds(row, col);
                    if (bounds != null) {
                        Bounds emiBounds = new Bounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
                        if (emiBounds.contains(x, y)) {
                            screen.presetPanel.getPresetData().setStack(row, col, itemStack);
                            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                new com.ggrgg.createredstonelinkgui.common.network.PresetSlotUpdatePayload(row, col, itemStack));
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void render(T screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        if (dragged == null || dragged.isEmpty()) return;

        int highlightColor = 0x8822BB33;

        // Highlight frequency slots
        Bounds slot1 = toBounds(screen.slot1Bounds);
        Bounds slot2 = toBounds(screen.slot2Bounds);
        if (slot1.contains(mouseX, mouseY)) {
            draw.fill(slot1.x(), slot1.y(), slot1.x() + slot1.width(), slot1.y() + slot1.height(), highlightColor);
        } else if (slot2.contains(mouseX, mouseY)) {
            draw.fill(slot2.x(), slot2.y(), slot2.x() + slot2.width(), slot2.y() + slot2.height(), highlightColor);
        }

        // Highlight preset slots
        if (screen.presetPanel != null) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 2; col++) {
                    Rect2i bounds = screen.presetPanel.getSlotBounds(row, col);
                    if (bounds != null && bounds.contains(mouseX, mouseY)) {
                        Bounds emiBounds = new Bounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
                        draw.fill(emiBounds.x(), emiBounds.y(), emiBounds.x() + emiBounds.width(), emiBounds.y() + emiBounds.height(), highlightColor);
                    }
                }
            }
        }
    }

    private static Bounds toBounds(net.minecraft.client.renderer.Rect2i rect) {
        return new Bounds(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }
}