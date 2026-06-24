package com.ggrgg.createredstonelinkgui.client.screen.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Panel displayed to the left of the link config screen.
 * Shows 4 rows of preset frequencies with copy/paste buttons.
 * Buttons grey out when their action would be meaningless:
 * - Copy greyed when both link frequency slots are empty
 * - Paste greyed when both preset slots in that row are empty
 */
public class FrequencyPresetPanel {

    // ==================== 面板尺寸 ====================
    public static final int PANEL_WIDTH = 65;
    public static final int PANEL_HEIGHT = 110;

    // ==================== 纹理资源 ====================
    private static final ResourceLocation PANEL_TEXTURE =
        ResourceLocation.parse("createredstonelinkgui:textures/gui/frequency_preset_panel.png");

    // ==================== 假脱机坐标（相对于面板左上角） ====================
    private static final int SLOT_X = 3;
    private static final int SLOT_Y_START = 16;
    private static final int SLOT_SPACING_Y = 22;
    private static final int SLOT_SIZE = 16;

    // ==================== 按钮坐标（相对于面板左上角） ====================
    private static final int COPY_BTN_X = 39;
    private static final int PASTE_BTN_X = 51;
    private static final int BTN_SIZE = 12;

    // ==================== 纹理UV坐标（当useTexture=true时使用） ====================
    private static final int PANEL_U = 0;
    private static final int PANEL_V = 0;
    private static final int SLOT_UV_U = 0;
    private static final int SLOT_UV_V = 112;
    private static final int COPY_BTN_U = 20;
    private static final int COPY_BTN_V = 112;
    private static final int COPY_BTN_HOVER_U = 36;
    private static final int COPY_BTN_HOVER_V = 112;
    private static final int PASTE_BTN_U = 52;
    private static final int PASTE_BTN_V = 112;
    private static final int PASTE_BTN_HOVER_U = 68;
    private static final int PASTE_BTN_HOVER_V = 112;

    // ==================== 纹理检测 ====================
    private static boolean isTextureAvailable() {
        return false;
    }

    // ==================== 实例字段 ====================
    private final FrequencyPresetData presetData;
    private final BlockPos linkPos;
    private final int panelX;
    private final int panelY;
    private final List<Rect2i> slotBounds;
    private final List<Rect2i> copyBtnBounds;
    private final List<Rect2i> pasteBtnBounds;
    private final Supplier<Boolean> copyEnabled; // evaluates true when link has frequency items

    /**
     * @param panelX Absolute screen X for the panel's top-left corner
     * @param panelY Absolute screen Y for the panel's top-left corner
     * @param linkPos Block position of the link being configured
     * @param presetData The player's preset data
     * @param copyEnabled Supplier that returns true if the link's frequency slots are not both empty
     */
    public FrequencyPresetPanel(int panelX, int panelY, BlockPos linkPos, FrequencyPresetData presetData,
                                Supplier<Boolean> copyEnabled) {
        this.panelX = panelX;
        this.panelY = panelY;
        this.linkPos = linkPos;
        this.presetData = presetData;
        this.copyEnabled = copyEnabled;
        this.slotBounds = new ArrayList<>(FrequencyPresetData.PRESET_COUNT * 2);
        this.copyBtnBounds = new ArrayList<>(FrequencyPresetData.PRESET_COUNT);
        this.pasteBtnBounds = new ArrayList<>(FrequencyPresetData.PRESET_COUNT);

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + SLOT_Y_START + row * SLOT_SPACING_Y;
            slotBounds.add(new Rect2i(panelX + SLOT_X, rowY, SLOT_SIZE, SLOT_SIZE));
            slotBounds.add(new Rect2i(panelX + SLOT_X + 18, rowY, SLOT_SIZE, SLOT_SIZE));
            copyBtnBounds.add(new Rect2i(panelX + COPY_BTN_X, rowY + 3, BTN_SIZE, BTN_SIZE));
            pasteBtnBounds.add(new Rect2i(panelX + PASTE_BTN_X, rowY + 3, BTN_SIZE, BTN_SIZE));
        }
    }

    // ==================== 状态查询 ====================
    private boolean isCopyGloballyEnabled() {
        return copyEnabled.get();
    }

    private boolean isPasteEnabled(int row) {
        return !presetData.getStack(row, 0).isEmpty()
            || !presetData.getStack(row, 1).isEmpty();
    }

    // ==================== 渲染 ====================
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isTextureAvailable()) {
            renderWithTexture(graphics, mouseX, mouseY);
        } else {
            renderFallback(graphics, mouseX, mouseY);
        }
    }

    private void renderWithTexture(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        boolean globalCopyEnabled = isCopyGloballyEnabled();

        graphics.blit(PANEL_TEXTURE, panelX, panelY, PANEL_U, PANEL_V, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

        Component title = Component.translatable("gui.createredstonelinkgui.presets");
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 4, 0xFFC8C8C8, false);

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + SLOT_Y_START + row * SLOT_SPACING_Y;
            graphics.drawString(font, String.valueOf(row + 1), panelX + SLOT_X - 10, rowY + 4, 0xFF888888, false);

            boolean pasteEnabled = isPasteEnabled(row);

            for (int col = 0; col < 2; col++) {
                int slotX = panelX + SLOT_X + col * 18;
                graphics.blit(PANEL_TEXTURE, slotX, rowY, SLOT_UV_U, SLOT_UV_V, SLOT_SIZE, SLOT_SIZE, 256, 256);
                ItemStack stack = presetData.getStack(row, col);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX, rowY);
                }
            }

            int copyBtnX = panelX + COPY_BTN_X;
            boolean copyHover = globalCopyEnabled && isHovered(mouseX, mouseY, copyBtnX, rowY + 3, BTN_SIZE, BTN_SIZE);
            graphics.blit(PANEL_TEXTURE, copyBtnX, rowY + 3,
                copyHover ? COPY_BTN_HOVER_U : COPY_BTN_U,
                copyHover ? COPY_BTN_HOVER_V : COPY_BTN_V,
                BTN_SIZE, BTN_SIZE, 256, 256);

            int pasteBtnX = panelX + PASTE_BTN_X;
            boolean pasteHover = pasteEnabled && isHovered(mouseX, mouseY, pasteBtnX, rowY + 3, BTN_SIZE, BTN_SIZE);
            graphics.blit(PANEL_TEXTURE, pasteBtnX, rowY + 3,
                pasteHover ? PASTE_BTN_HOVER_U : PASTE_BTN_U,
                pasteHover ? PASTE_BTN_HOVER_V : PASTE_BTN_V,
                BTN_SIZE, BTN_SIZE, 256, 256);
        }
    }

    private void renderFallback(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        boolean globalCopyEnabled = isCopyGloballyEnabled();

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC333333);

        Component title = Component.translatable("gui.createredstonelinkgui.presets");
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 4, 0xFFC8C8C8, false);

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + SLOT_Y_START + row * SLOT_SPACING_Y;

            graphics.drawString(font, String.valueOf(row + 1), panelX + SLOT_X - 10, rowY + 4, 0xFF888888, false);

            for (int col = 0; col < 2; col++) {
                int slotX = panelX + SLOT_X + col * 18;
                drawSlotBackground(graphics, slotX, rowY);
                ItemStack stack = presetData.getStack(row, col);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX, rowY);
                }
            }

            boolean pasteEnabled = isPasteEnabled(row);
            int copyBtnX = panelX + COPY_BTN_X;
            int pasteBtnX = panelX + PASTE_BTN_X;

            boolean copyHover = globalCopyEnabled && isHovered(mouseX, mouseY, copyBtnX, rowY + 3, BTN_SIZE, BTN_SIZE);
            boolean pasteHover = pasteEnabled && isHovered(mouseX, mouseY, pasteBtnX, rowY + 3, BTN_SIZE, BTN_SIZE);

            drawButton(graphics, copyBtnX, rowY + 3, "C", copyHover, globalCopyEnabled);
            drawButton(graphics, pasteBtnX, rowY + 3, "P", pasteHover, pasteEnabled);
        }
    }

    // ==================== 工具提示 ====================
    public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
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

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            Rect2i bounds = copyBtnBounds.get(row);
            if (bounds.contains(mouseX, mouseY)) {
                if (!isCopyGloballyEnabled()) return; // greyed out, no tooltip
                graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("gui.createredstonelinkgui.copy_to_preset", row + 1)
                        .withStyle(ChatFormatting.GREEN),
                    mouseX, mouseY);
                return;
            }
        }

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            Rect2i bounds = pasteBtnBounds.get(row);
            if (bounds.contains(mouseX, mouseY)) {
                if (!isPasteEnabled(row)) return; // greyed out, no tooltip
                graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("gui.createredstonelinkgui.paste_from_preset", row + 1)
                        .withStyle(ChatFormatting.GOLD),
                    mouseX, mouseY);
                return;
            }
        }
    }

    // ==================== 鼠标点击 ====================
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;

        boolean globalCopyEnabled = isCopyGloballyEnabled();

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            if (globalCopyEnabled && copyBtnBounds.get(row).contains(mx, my)) {
                PacketDistributor.sendToServer(new CopyToPresetPayload(linkPos, row));
                return true;
            }
            if (isPasteEnabled(row) && pasteBtnBounds.get(row).contains(mx, my)) {
                PacketDistributor.sendToServer(new PasteFromPresetPayload(linkPos, row));
                return true;
            }
        }

        return false;
    }

    // ==================== 辅助绘制方法 ====================
    private void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0xFF555555);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF333333);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, String label, boolean hovered, boolean enabled) {
        if (!enabled) {
            // Greyed out but still visible
            graphics.fill(x - 1, y - 1, x + BTN_SIZE + 1, y + BTN_SIZE + 1, 0xFF444444);
            graphics.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, 0xFF333333);
            graphics.fill(x, y + BTN_SIZE - 2, x + BTN_SIZE, y + BTN_SIZE - 1, 0xFF555555);
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, label, x + 2, y + 1, 0xFF888888, false);
            return;
        }
        // Enabled - bright colors
        int bgColor = hovered ? 0xFF66AAFF : 0xFF4488EE;
        int borderColor = hovered ? 0xFFCCEEFF : 0xFFAACCFF;
        int textColor = 0xFFFFFFFF;
        graphics.fill(x - 1, y - 1, x + BTN_SIZE + 1, y + BTN_SIZE + 1, borderColor);
        graphics.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, bgColor);
        graphics.fill(x, y + BTN_SIZE - 2, x + BTN_SIZE, y + BTN_SIZE - 1,
            label.equals("C") ? 0xFF00FF00 : 0xFF00AAFF);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, label, x + 2, y + 1, textColor, false);
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    // ==================== 获取槽位边界 ====================
    public Rect2i getSlotBounds(int row, int col) {
        int index = row * 2 + col;
        if (index < 0 || index >= slotBounds.size()) return null;
        return slotBounds.get(index);
    }

    // ==================== 获取面板边界 ====================
    public Rect2i getBounds() {
        return new Rect2i(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
    }

    public int getPanelX() { return panelX; }
    public int getPanelY() { return panelY; }
    public FrequencyPresetData getPresetData() { return presetData; }
    public BlockPos getLinkPos() { return linkPos; }
}