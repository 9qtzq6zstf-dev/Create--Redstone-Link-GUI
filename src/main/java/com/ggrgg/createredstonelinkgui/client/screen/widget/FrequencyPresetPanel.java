package com.ggrgg.createredstonelinkgui.client.screen.widget;

import java.util.ArrayList;
import java.util.List;

import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;
import com.ggrgg.createredstonelinkgui.common.network.CopyToPresetPayload;
import com.ggrgg.createredstonelinkgui.common.network.PasteFromPresetPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Panel displayed on the left side of the link config screen.
 * Shows 4 rows of preset frequencies with copy/paste buttons.
 */
public class FrequencyPresetPanel {

    // Panel dimensions
    public static final int PANEL_WIDTH = 65;
    public static final int PANEL_HEIGHT = 110;
    public static final int PANEL_X_OFFSET = 5;
    public static final int PANEL_Y_OFFSET = 5;

    // Slot positions relative to panel top-left
    public static final int SLOT_X = 3;
    public static final int SLOT_Y_START = 16;
    public static final int SLOT_SPACING_Y = 22;
    public static final int SLOT_SIZE = 16;

    // Button positions relative to panel top-left
    public static final int COPY_BTN_X = 39;
    public static final int PASTE_BTN_X = 51;
    public static final int BTN_SIZE = 10;

    // Texture UV for copy/paste icons (reuse from overlay texture)
    // Copy = arrow pointing right, Paste = arrow pointing in
    // We'll use simple text characters for now since we don't have custom textures
    private static final int SLOT_UV_X = 77;
    private static final int SLOT_UV_Y = 188;
    private static final int SLOT_BG_UV_X = 112;
    private static final int SLOT_BG_UV_Y = 172;

    private final FrequencyPresetData presetData;
    private final BlockPos linkPos;
    private final int panelX;
    private final int panelY;
    private final List<Rect2i> slotBounds;
    private final List<Rect2i> copyBtnBounds;
    private final List<Rect2i> pasteBtnBounds;

    /**
     * @param panelX Absolute screen X for the panel's top-left corner
     * @param panelY Absolute screen Y for the panel's top-left corner
     * @param linkPos Block position of the link being configured
     * @param presetData The player's preset data
     */
    public FrequencyPresetPanel(int panelX, int panelY, BlockPos linkPos, FrequencyPresetData presetData) {
        this.panelX = panelX;
        this.panelY = panelY;
        this.linkPos = linkPos;
        this.presetData = presetData;
        this.slotBounds = new ArrayList<>(FrequencyPresetData.PRESET_COUNT * 2);
        this.copyBtnBounds = new ArrayList<>(FrequencyPresetData.PRESET_COUNT);
        this.pasteBtnBounds = new ArrayList<>(FrequencyPresetData.PRESET_COUNT);

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + SLOT_Y_START + row * SLOT_SPACING_Y;
            // Slot 0
            slotBounds.add(new Rect2i(panelX + SLOT_X, rowY, SLOT_SIZE, SLOT_SIZE));
            // Slot 1
            slotBounds.add(new Rect2i(panelX + SLOT_X + 18, rowY, SLOT_SIZE, SLOT_SIZE));
            // Copy button
            copyBtnBounds.add(new Rect2i(panelX + COPY_BTN_X, rowY + 3, BTN_SIZE, BTN_SIZE));
            // Paste button
            pasteBtnBounds.add(new Rect2i(panelX + PASTE_BTN_X, rowY + 3, BTN_SIZE, BTN_SIZE));
        }
    }

    /**
     * Render the preset panel.
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        // Draw panel background
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC333333);

        // Draw title
        Component title = Component.translatable("gui.createredstonelinkgui.presets");
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 4, 0xFFC8C8C8, false);

        // Draw each row
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + SLOT_Y_START + row * SLOT_SPACING_Y;

            // Draw row number label
            graphics.drawString(font, String.valueOf(row + 1), panelX + SLOT_X - 10, rowY + 4, 0xFF888888, false);

            // Draw slot backgrounds and items
            for (int col = 0; col < 2; col++) {
                int slotX = panelX + SLOT_X + col * 18;
                drawSlotBackground(graphics, slotX, rowY);
                ItemStack stack = presetData.getStack(row, col);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX, rowY);
                }
            }

            // Draw copy button
            int copyBtnX = panelX + COPY_BTN_X;
            drawButton(graphics, copyBtnX, rowY + 3, "C",
                isHovered(mouseX, mouseY, copyBtnX, rowY + 3, BTN_SIZE, BTN_SIZE));

            // Draw paste button
            int pasteBtnX = panelX + PASTE_BTN_X;
            drawButton(graphics, pasteBtnX, rowY + 3, "P",
                isHovered(mouseX, mouseY, pasteBtnX, rowY + 3, BTN_SIZE, BTN_SIZE));
        }
    }

    /**
     * Render tooltips for hovered elements.
     */
    public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        // Check slot tooltips
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            for (int col = 0; col < 2; col++) {
                Rect2i bounds = slotBounds.get(row * 2 + col);
                if (bounds.contains(mouseX, mouseY)) {
                    ItemStack stack = presetData.getStack(row, col);
                    if (!stack.isEmpty()) {
                        graphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
                    } else {
                        graphics.renderTooltip(Minecraft.getInstance().font,
                            Component.translatable("gui.createredstonelinkgui.preset_slot_empty",
                                row + 1, col + 1).withStyle(ChatFormatting.GRAY),
                            mouseX, mouseY);
                    }
                    return;
                }
            }
        }

        // Check copy button tooltips
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            Rect2i bounds = copyBtnBounds.get(row);
            if (bounds.contains(mouseX, mouseY)) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("gui.createredstonelinkgui.copy_to_preset", row + 1)
                        .withStyle(ChatFormatting.GREEN),
                    mouseX, mouseY);
                return;
            }
        }

        // Check paste button tooltips
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            Rect2i bounds = pasteBtnBounds.get(row);
            if (bounds.contains(mouseX, mouseY)) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("gui.createredstonelinkgui.paste_from_preset", row + 1)
                        .withStyle(ChatFormatting.GOLD),
                    mouseX, mouseY);
                return;
            }
        }
    }

    /**
     * Handle mouse clicks. Returns true if the click was handled.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false; // Only left clicks

        int mx = (int) mouseX;
        int my = (int) mouseY;

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            // Copy button
            if (copyBtnBounds.get(row).contains(mx, my)) {
                PacketDistributor.sendToServer(new CopyToPresetPayload(linkPos, row));
                return true;
            }
            // Paste button
            if (pasteBtnBounds.get(row).contains(mx, my)) {
                PacketDistributor.sendToServer(new PasteFromPresetPayload(linkPos, row));
                return true;
            }
        }

        return false;
    }

    private void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        // Draw a simple dark square with border (mimicking a slot)
        graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0xFF555555);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF333333);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, String label, boolean hovered) {
        int bgColor = hovered ? 0xFF5555CC : 0xFF444444;
        int borderColor = hovered ? 0xFF8888FF : 0xFF666666;
        graphics.fill(x - 1, y - 1, x + BTN_SIZE + 1, y + BTN_SIZE + 1, borderColor);
        graphics.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, bgColor);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, label, x + 1, y + 1, hovered ? 0xFFFFFFFF : 0xFFCCCCCC, false);
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    /**
     * Get the menu slot x position for a preset slot.
     * This should match what the menu uses.
     */
    public static int getMenuSlotX(int col) {
        // Relative to contentLeft position
        return 1; // Will be adjusted
    }

    /**
     * Get the menu slot y position for a preset slot (relative to contentTop).
     */
    public static int getMenuSlotY(int row) {
        // Relative to contentTop position
        return SLOT_Y_START + row * SLOT_SPACING_Y;
    }
}