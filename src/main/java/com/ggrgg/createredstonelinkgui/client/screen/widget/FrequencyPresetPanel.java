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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Panel displayed to the left of the link config screen.
 * Shows rows of preset frequencies with copy/paste buttons.
 *
 * <h2>Texture Layout (frequency_preset_panel.png, 256×256)</h2>
 * <pre>
 *   HEADER   U=0, V=0   W=65, H=16   — Panel top + "Presets" title background
 *   ROW      U=0, V=16  W=65, H=22   — Repeated per row, slot/button area
 *   FOOTER   U=0, V=204 W=65, H=10   — Panel bottom edge border
 *
 * Slot and button sub-regions within the ROW segment (at U=0, V=16):
 *   Slot 0     X=3..19   (16×16)  — First frequency slot position
 *   Slot 1     X=21..37  (16×16)  — Second frequency slot position
 *   Copy btn   X=39..50  (12×12)  — Copy-to-preset button
 *   Paste btn  X=51..62  (12×12)  — Paste-from-preset button
 *
 * Button hover variants are at V=38 (ROW_V + ROW_H = 16 + 22):
 *   Copy hover   X=39, Y=38  (12×12)
 *   Paste hover  X=51, Y=38  (12×12)
 * </pre>
 *
 * All 256×256 texture coordinates are available for custom slot/button icons.
 */
public class FrequencyPresetPanel {

    // ==================== 布局常量 ====================
    private static final int SLOT_X = 3;
    private static final int SLOT_Y_OFFSET = 3; // Y within a row segment
    private static final int SLOT_SPACING_X = 18;
    private static final int SLOT_SPACING_Y = 22;
    private static final int SLOT_SIZE = 16;
    private static final int COPY_BTN_X = 39;
    private static final int PASTE_BTN_X = 51;
    private static final int BTN_Y_OFFSET = 3;
    private static final int BTN_SIZE = 12;

    // ==================== 面板尺寸（自动计算） ====================
    public static final int PANEL_WIDTH = 65;
    public static final int HEADER_HEIGHT = 16;
    public static final int ROW_HEIGHT = 22;
    public static final int FOOTER_HEIGHT = 10;
    public static final int PANEL_HEIGHT =
        HEADER_HEIGHT + FrequencyPresetData.PRESET_COUNT * ROW_HEIGHT + FOOTER_HEIGHT;

    // ==================== 纹理资源 ====================
    private static final ResourceLocation PANEL_TEXTURE =
        ResourceLocation.parse("createredstonelinkgui:textures/gui/frequency_preset_panel.png");

    // ==================== 纹理UV坐标 — 面板分段 ====================
    private static final int HEADER_U = 0;
    private static final int HEADER_V = 0;
    private static final int ROW_U = 0;
    private static final int ROW_V = 16;
    private static final int FOOTER_U = 0;
    private static final int FOOTER_V = 204;

    // ==================== 纹理UV坐标 — 槽位/按钮（ROW段内） ====================
    private static final int SLOT_UV_U = 3;
    private static final int SLOT_UV_V = 16;
    private static final int COPY_BTN_UV_U = 39;
    private static final int COPY_BTN_UV_V = 16;
    private static final int PASTE_BTN_UV_U = 51;
    private static final int PASTE_BTN_UV_V = 16;
    private static final int COPY_BTN_HOVER_UV_U = 39;
    private static final int COPY_BTN_HOVER_UV_V = 38; // ROW_V + ROW_H
    private static final int PASTE_BTN_HOVER_UV_U = 51;
    private static final int PASTE_BTN_HOVER_UV_V = 38;

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
    private final Supplier<Boolean> copyEnabled;

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
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            slotBounds.add(new Rect2i(panelX + SLOT_X, rowY + SLOT_Y_OFFSET, SLOT_SIZE, SLOT_SIZE));
            slotBounds.add(new Rect2i(panelX + SLOT_X + SLOT_SPACING_X, rowY + SLOT_Y_OFFSET, SLOT_SIZE, SLOT_SIZE));
            copyBtnBounds.add(new Rect2i(panelX + COPY_BTN_X, rowY + BTN_Y_OFFSET, BTN_SIZE, BTN_SIZE));
            pasteBtnBounds.add(new Rect2i(panelX + PASTE_BTN_X, rowY + BTN_Y_OFFSET, BTN_SIZE, BTN_SIZE));
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

    // ==================== 渲染入口 ====================
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isTextureAvailable()) {
            renderWithTexture(graphics, mouseX, mouseY);
        } else {
            renderFallback(graphics, mouseX, mouseY);
        }
    }

    // ==================== 纹理渲染路径（两遍：先背景再内容） ====================
    private void renderWithTexture(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        boolean globalCopyEnabled = isCopyGloballyEnabled();

        // === 第一遍：所有背景段 ===
        // 面板头部
        graphics.blit(PANEL_TEXTURE, panelX, panelY, HEADER_U, HEADER_V, PANEL_WIDTH, HEADER_HEIGHT, 256, 256);
        // 每行
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            graphics.blit(PANEL_TEXTURE, panelX, rowY, ROW_U, ROW_V, PANEL_WIDTH, ROW_HEIGHT, 256, 256);
        }
        // 面板底部
        int footerY = panelY + HEADER_HEIGHT + FrequencyPresetData.PRESET_COUNT * ROW_HEIGHT;
        graphics.blit(PANEL_TEXTURE, panelX, footerY, FOOTER_U, FOOTER_V, PANEL_WIDTH, FOOTER_HEIGHT, 256, 256);

        // === 第二遍：所有内容层 ===
        // 标题
        Component title = Component.translatable("gui.createredstonelinkgui.presets");
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 4, 0xFFC8C8C8, false);

        // 每行内容
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            boolean pasteEnabled = isPasteEnabled(row);

            // 行号
            graphics.drawString(font, String.valueOf(row + 1), panelX + SLOT_X - 10, rowY + SLOT_Y_OFFSET + 4, 0xFF888888, false);

            // 槽位背景 + 物品
            for (int col = 0; col < 2; col++) {
                int slotX = panelX + SLOT_X + col * SLOT_SPACING_X;
                graphics.blit(PANEL_TEXTURE, slotX, rowY + SLOT_Y_OFFSET, SLOT_UV_U, SLOT_UV_V, SLOT_SIZE, SLOT_SIZE, 256, 256);
                ItemStack stack = presetData.getStack(row, col);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX, rowY + SLOT_Y_OFFSET);
                }
            }

            // 复制按钮
            int copyBtnX = panelX + COPY_BTN_X;
            boolean copyHover = globalCopyEnabled && isHovered(mouseX, mouseY, copyBtnX, rowY + BTN_Y_OFFSET, BTN_SIZE, BTN_SIZE);
            graphics.blit(PANEL_TEXTURE, copyBtnX, rowY + BTN_Y_OFFSET,
                copyHover ? COPY_BTN_HOVER_UV_U : COPY_BTN_UV_U,
                copyHover ? COPY_BTN_HOVER_UV_V : COPY_BTN_UV_V,
                BTN_SIZE, BTN_SIZE, 256, 256);

            // 粘贴按钮
            int pasteBtnX = panelX + PASTE_BTN_X;
            boolean pasteHover = pasteEnabled && isHovered(mouseX, mouseY, pasteBtnX, rowY + BTN_Y_OFFSET, BTN_SIZE, BTN_SIZE);
            graphics.blit(PANEL_TEXTURE, pasteBtnX, rowY + BTN_Y_OFFSET,
                pasteHover ? PASTE_BTN_HOVER_UV_U : PASTE_BTN_UV_U,
                pasteHover ? PASTE_BTN_HOVER_UV_V : PASTE_BTN_UV_V,
                BTN_SIZE, BTN_SIZE, 256, 256);
        }
    }

    // ==================== 回退渲染路径（两遍：先背景再内容） ====================
    private void renderFallback(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        boolean globalCopyEnabled = isCopyGloballyEnabled();

        // === 第一遍：所有背景段 ===
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT, 0xCC333333);
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            graphics.fill(panelX, rowY, panelX + PANEL_WIDTH, rowY + ROW_HEIGHT,
                (row % 2 == 0) ? 0xCC2A2A2A : 0xCC333333);
        }
        int footerY = panelY + HEADER_HEIGHT + FrequencyPresetData.PRESET_COUNT * ROW_HEIGHT;
        graphics.fill(panelX, footerY, panelX + PANEL_WIDTH, footerY + FOOTER_HEIGHT, 0xCC222222);

        // === 第二遍：所有内容层 ===
        Component title = Component.translatable("gui.createredstonelinkgui.presets");
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 4, 0xFFC8C8C8, false);

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            boolean pasteEnabled = isPasteEnabled(row);

            graphics.drawString(font, String.valueOf(row + 1), panelX + SLOT_X - 10, rowY + SLOT_Y_OFFSET + 4, 0xFF888888, false);

            for (int col = 0; col < 2; col++) {
                int slotX = panelX + SLOT_X + col * SLOT_SPACING_X;
                drawSlotBackground(graphics, slotX, rowY + SLOT_Y_OFFSET);
                ItemStack stack = presetData.getStack(row, col);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX, rowY + SLOT_Y_OFFSET);
                }
            }

            int copyBtnX = panelX + COPY_BTN_X;
            boolean copyHover = globalCopyEnabled && isHovered(mouseX, mouseY, copyBtnX, rowY + BTN_Y_OFFSET, BTN_SIZE, BTN_SIZE);
            drawButton(graphics, copyBtnX, rowY + BTN_Y_OFFSET, "C", copyHover, globalCopyEnabled);

            int pasteBtnX = panelX + PASTE_BTN_X;
            boolean pasteHover = pasteEnabled && isHovered(mouseX, mouseY, pasteBtnX, rowY + BTN_Y_OFFSET, BTN_SIZE, BTN_SIZE);
            drawButton(graphics, pasteBtnX, rowY + BTN_Y_OFFSET, "P", pasteHover, pasteEnabled);
        }
    }

    // ==================== 频率符号检测 ====================
    private static boolean isFrequencySymbol(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("frequency")) return false;
        String path = id.getPath();
        if (!path.startsWith("symbol_")) return false;
        if (path.equals("symbol_frame")) return false;
        return true;
    }

    // ==================== 工具提示 ====================
    public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            for (int col = 0; col < 2; col++) {
                Rect2i bounds = slotBounds.get(row * 2 + col);
                if (bounds.contains(mouseX, mouseY)) {
                    ItemStack stack = presetData.getStack(row, col);
                    Component label = Component.translatable(
                        col == 0 ? "gui.createredstonelinkgui.frequency_first"
                                 : "gui.createredstonelinkgui.frequency_second")
                        .withStyle(ChatFormatting.BLUE);
                    if (!stack.isEmpty()) {
                        List<Component> tooltipLines = new ArrayList<>();
                        tooltipLines.add(label);
                        int lineCount = 1;
                        if (isFrequencySymbol(stack)) {
                            tooltipLines.add(Component.translatable("gui.createredstonelinkgui.middle_click_swap")
                                    .withStyle(ChatFormatting.GOLD));
                            lineCount = 2;
                        }
                        int yOffset = -20 - (lineCount - 1) * font.lineHeight;
                        graphics.renderTooltip(font, tooltipLines,
                            java.util.Optional.empty(), mouseX, mouseY + yOffset);
                    } else {
                        int yOffset = -20;
                        graphics.renderTooltip(font,
                            java.util.List.of(label), java.util.Optional.empty(), mouseX, mouseY + yOffset);
                    }
                    return;
                }
            }
        }

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            Rect2i bounds = copyBtnBounds.get(row);
            if (bounds.contains(mouseX, mouseY)) {
                if (!isCopyGloballyEnabled()) return;
                graphics.renderTooltip(font,
                    Component.translatable("gui.createredstonelinkgui.copy_to_preset", row + 1)
                        .withStyle(ChatFormatting.GREEN),
                    mouseX, mouseY);
                return;
            }
        }

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            Rect2i bounds = pasteBtnBounds.get(row);
            if (bounds.contains(mouseX, mouseY)) {
                if (!isPasteEnabled(row)) return;
                graphics.renderTooltip(font,
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
            graphics.fill(x - 1, y - 1, x + BTN_SIZE + 1, y + BTN_SIZE + 1, 0xFF444444);
            graphics.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, 0xFF333333);
            graphics.fill(x, y + BTN_SIZE - 2, x + BTN_SIZE, y + BTN_SIZE - 1, 0xFF555555);
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, label, x + 2, y + 1, 0xFF888888, false);
            return;
        }
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

    // ==================== 边界获取 ====================
    public Rect2i getSlotBounds(int row, int col) {
        int index = row * 2 + col;
        if (index < 0 || index >= slotBounds.size()) return null;
        return slotBounds.get(index);
    }

    public Rect2i getBounds() {
        return new Rect2i(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
    }

    public int getPanelX() { return panelX; }
    public int getPanelY() { return panelY; }
    public FrequencyPresetData getPresetData() { return presetData; }
    public BlockPos getLinkPos() { return linkPos; }
}