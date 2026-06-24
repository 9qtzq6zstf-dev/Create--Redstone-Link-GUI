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
 *   HEADER   U=0, V=0    W=90, H=21  — Panel top + "Presets" title background
 *   ROW      U=0, V=21   W=90, H=27  — Repeated per row
 *   FOOTER   U=0, V=129  W=90, H=13  — Panel bottom edge
 *
 * Sub-regions within ROW segment (at V=21):
 *   Slot 0 outer  X=2,  Y=21, W=23, H=23  (inner 16×16 item at +3,+3)
 *   Slot 1 outer  X=27, Y=21, W=23, H=23
 *   Copy btn      X=52, Y=21, W=18, H=18
 *   Paste btn     X=72, Y=21, W=18, H=18
 *   Copy hover    X=52, Y=48 (21+27)
 *   Paste hover   X=72, Y=48
 * </pre>
 */
public class FrequencyPresetPanel {

    // ==================== 布局常量 ====================
    private static final int SLOT_X = 2;
    private static final int SLOT_Y_OFFSET = 4;       // Y within a row segment
    private static final int SLOT_OUTER_SIZE = 23;     // outer bounding box (16 item + 5 extra = 21, + 2 border = 23)
    private static final int SLOT_SPACING_X = 25;      // 23 outer + 2 gap
    private static final int SLOT_INNER_SIZE = 16;     // inner slot visual
    private static final int ITEM_OFFSET = 3;          // center 16×16 item in 23×23: (23-16)/2 ≈ 3
    private static final int COPY_BTN_X = 52;
    private static final int PASTE_BTN_X = 72;
    private static final int BTN_Y_OFFSET = 5;         // centered in 27px row, 2px down from previous
    private static final int BTN_SIZE = 18;

    // ==================== 面板尺寸 ====================
    public static final int PANEL_WIDTH = 90;
    public static final int HEADER_HEIGHT = 21;
    public static final int ROW_HEIGHT = 27;
    public static final int FOOTER_HEIGHT = 13;
    public static final int PANEL_HEIGHT =
        HEADER_HEIGHT + FrequencyPresetData.PRESET_COUNT * ROW_HEIGHT + FOOTER_HEIGHT;

    // ==================== 纹理资源 ====================
    private static final ResourceLocation PANEL_TEXTURE =
        ResourceLocation.parse("createredstonelinkgui:textures/gui/frequency_preset_panel.png");

    // ==================== 纹理UV坐标 — 面板分段 ====================
    private static final int HEADER_U = 0;
    private static final int HEADER_V = 0;
    private static final int ROW_U = 0;
    private static final int ROW_V = 21;
    private static final int FOOTER_U = 0;
    private static final int FOOTER_V = 129;

    // ==================== 纹理UV坐标 — 槽位/按钮 ====================
    private static final int SLOT_OUTER_UV_U = 2;
    private static final int SLOT_OUTER_UV_V = 21;
    private static final int COPY_BTN_UV_U = 52;
    private static final int COPY_BTN_UV_V = 21;
    private static final int PASTE_BTN_UV_U = 72;
    private static final int PASTE_BTN_UV_V = 21;
    private static final int COPY_BTN_HOVER_UV_U = 52;
    private static final int COPY_BTN_HOVER_UV_V = 48; // 21 + 27
    private static final int PASTE_BTN_HOVER_UV_U = 72;
    private static final int PASTE_BTN_HOVER_UV_V = 48;

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
            slotBounds.add(new Rect2i(panelX + SLOT_X, rowY + SLOT_Y_OFFSET, SLOT_OUTER_SIZE, SLOT_OUTER_SIZE));
            slotBounds.add(new Rect2i(panelX + SLOT_X + SLOT_SPACING_X, rowY + SLOT_Y_OFFSET, SLOT_OUTER_SIZE, SLOT_OUTER_SIZE));
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

    // ==================== 纹理渲染路径 ====================
    private void renderWithTexture(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        boolean globalCopyEnabled = isCopyGloballyEnabled();

        // PASS 1: backgrounds
        graphics.blit(PANEL_TEXTURE, panelX, panelY, HEADER_U, HEADER_V, PANEL_WIDTH, HEADER_HEIGHT, 256, 256);
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            graphics.blit(PANEL_TEXTURE, panelX, rowY, ROW_U, ROW_V, PANEL_WIDTH, ROW_HEIGHT, 256, 256);
        }
        int footerY = panelY + HEADER_HEIGHT + FrequencyPresetData.PRESET_COUNT * ROW_HEIGHT;
        graphics.blit(PANEL_TEXTURE, panelX, footerY, FOOTER_U, FOOTER_V, PANEL_WIDTH, FOOTER_HEIGHT, 256, 256);

        // PASS 2: content
        Component title = Component.translatable("gui.createredstonelinkgui.presets");
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 4, 0xFFC8C8C8, false);

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            boolean pasteEnabled = isPasteEnabled(row);

            graphics.drawString(font, String.valueOf(row + 1), panelX + SLOT_X - 12, rowY + SLOT_Y_OFFSET + 5, 0xFF888888, false);

            for (int col = 0; col < 2; col++) {
                int slotX = panelX + SLOT_X + col * SLOT_SPACING_X;
                // Draw outer slot bounding box
                graphics.blit(PANEL_TEXTURE, slotX, rowY + SLOT_Y_OFFSET, SLOT_OUTER_UV_U, SLOT_OUTER_UV_V, SLOT_OUTER_SIZE, SLOT_OUTER_SIZE, 256, 256);
                ItemStack stack = presetData.getStack(row, col);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX + ITEM_OFFSET, rowY + SLOT_Y_OFFSET + ITEM_OFFSET);
                }
            }

            int copyBtnX = panelX + COPY_BTN_X;
            boolean copyHover = globalCopyEnabled && isHovered(mouseX, mouseY, copyBtnX, rowY + BTN_Y_OFFSET, BTN_SIZE, BTN_SIZE);
            graphics.blit(PANEL_TEXTURE, copyBtnX, rowY + BTN_Y_OFFSET,
                copyHover ? COPY_BTN_HOVER_UV_U : COPY_BTN_UV_U,
                copyHover ? COPY_BTN_HOVER_UV_V : COPY_BTN_UV_V,
                BTN_SIZE, BTN_SIZE, 256, 256);

            int pasteBtnX = panelX + PASTE_BTN_X;
            boolean pasteHover = pasteEnabled && isHovered(mouseX, mouseY, pasteBtnX, rowY + BTN_Y_OFFSET, BTN_SIZE, BTN_SIZE);
            graphics.blit(PANEL_TEXTURE, pasteBtnX, rowY + BTN_Y_OFFSET,
                pasteHover ? PASTE_BTN_HOVER_UV_U : PASTE_BTN_UV_U,
                pasteHover ? PASTE_BTN_HOVER_UV_V : PASTE_BTN_UV_V,
                BTN_SIZE, BTN_SIZE, 256, 256);
        }
    }

    // ==================== 回退渲染路径 ====================
    private void renderFallback(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        boolean globalCopyEnabled = isCopyGloballyEnabled();

        // PASS 1: backgrounds
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT, 0xCC333333);
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            graphics.fill(panelX, rowY, panelX + PANEL_WIDTH, rowY + ROW_HEIGHT,
                (row % 2 == 0) ? 0xCC2A2A2A : 0xCC333333);
        }
        int footerY = panelY + HEADER_HEIGHT + FrequencyPresetData.PRESET_COUNT * ROW_HEIGHT;
        graphics.fill(panelX, footerY, panelX + PANEL_WIDTH, footerY + FOOTER_HEIGHT, 0xCC222222);

        // PASS 2: content
        Component title = Component.translatable("gui.createredstonelinkgui.presets");
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 4, 0xFFC8C8C8, false);

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + HEADER_HEIGHT + row * ROW_HEIGHT;
            boolean pasteEnabled = isPasteEnabled(row);

            graphics.drawString(font, String.valueOf(row + 1), panelX + SLOT_X - 12, rowY + SLOT_Y_OFFSET + 5, 0xFF888888, false);

            for (int col = 0; col < 2; col++) {
                int slotX = panelX + SLOT_X + col * SLOT_SPACING_X;
                drawSlotBackground(graphics, slotX, rowY + SLOT_Y_OFFSET);
                ItemStack stack = presetData.getStack(row, col);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX + ITEM_OFFSET, rowY + SLOT_Y_OFFSET + ITEM_OFFSET);
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
        // Outer 23×23 bounding box
        graphics.fill(x, y, x + SLOT_OUTER_SIZE, y + SLOT_OUTER_SIZE, 0xFF555555);
        // Inner "normal inventory slot" 16×16 centered (offset 3 in 23)
        graphics.fill(x + ITEM_OFFSET, y + ITEM_OFFSET, x + ITEM_OFFSET + SLOT_INNER_SIZE, y + ITEM_OFFSET + SLOT_INNER_SIZE, 0xFF333333);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, String label, boolean hovered, boolean enabled) {
        if (!enabled) {
            graphics.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, 0xFF444444);
            graphics.fill(x + 1, y + 1, x + BTN_SIZE - 1, y + BTN_SIZE - 1, 0xFF333333);
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, label, x + 4, y + 3, 0xFF888888, false);
            return;
        }
        graphics.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, 0xFF4488EE);
        graphics.fill(x + 1, y + 1, x + BTN_SIZE - 1, y + BTN_SIZE - 1,
            hovered ? 0xFF77BBFF : 0xFF5599FF);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, label, x + 4, y + 3, 0xFFFFFFFF, false);
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