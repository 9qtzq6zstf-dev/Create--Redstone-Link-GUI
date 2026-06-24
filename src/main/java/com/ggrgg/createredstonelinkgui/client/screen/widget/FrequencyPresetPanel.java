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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Panel displayed to the left of the link config screen.
 * Shows 4 rows of preset frequencies with copy/paste buttons.
 * Supports a custom texture (frequency_preset_panel.png) with fill() fallback.
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
    private static final int BTN_SIZE = 10;

    // ==================== 纹理UV坐标（当useTexture=true时使用） ====================
    // 面板背景区域 (UV)
    private static final int PANEL_U = 0;
    private static final int PANEL_V = 0;
    // 槽位背景 (16×16)
    private static final int SLOT_UV_U = 0;
    private static final int SLOT_UV_V = 112;
    // 复制按钮 (默认 + 悬浮)
    private static final int COPY_BTN_U = 20;
    private static final int COPY_BTN_V = 112;
    private static final int COPY_BTN_HOVER_U = 36;
    private static final int COPY_BTN_HOVER_V = 112;
    // 粘贴按钮 (默认 + 悬浮)
    private static final int PASTE_BTN_U = 52;
    private static final int PASTE_BTN_V = 112;
    private static final int PASTE_BTN_HOVER_U = 68;
    private static final int PASTE_BTN_HOVER_V = 112;

    // ==================== 纹理检测 ====================
    private static Boolean textureAvailable = null;

    private static boolean isTextureAvailable() {
        if (textureAvailable == null) {
            try {
                Minecraft.getInstance().getTextureManager().getTexture(PANEL_TEXTURE);
                textureAvailable = true;
            } catch (Exception e) {
                textureAvailable = false;
            }
        }
        return textureAvailable;
    }

    // ==================== 实例字段 ====================
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

    // ==================== 渲染 ====================
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

        // 面板背景
        graphics.blit(PANEL_TEXTURE, panelX, panelY, PANEL_U, PANEL_V, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

        // 标题
        Component title = Component.translatable("gui.createredstonelinkgui.presets");
        int titleWidth = font.width(title);
        graphics.drawString(font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 4, 0xFFC8C8C8, false);

        // 每行
        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            int rowY = panelY + SLOT_Y_START + row * SLOT_SPACING_Y;

            // 行号
            graphics.drawString(font, String.valueOf(row + 1), panelX + SLOT_X - 10, rowY + 4, 0xFF888888, false);

            // 槽位
            for (int col = 0; col < 2; col++) {
                int slotX = panelX + SLOT_X + col * 18;
                graphics.blit(PANEL_TEXTURE, slotX, rowY, SLOT_UV_U, SLOT_UV_V, SLOT_SIZE, SLOT_SIZE, 256, 256);
                ItemStack stack = presetData.getStack(row, col);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX, rowY);
                }
            }

            // 复制按钮
            int copyBtnX = panelX + COPY_BTN_X;
            boolean copyHover = isHovered(mouseX, mouseY, copyBtnX, rowY + 3, BTN_SIZE, BTN_SIZE);
            graphics.blit(PANEL_TEXTURE, copyBtnX, rowY + 3,
                copyHover ? COPY_BTN_HOVER_U : COPY_BTN_U,
                copyHover ? COPY_BTN_HOVER_V : COPY_BTN_V,
                BTN_SIZE, BTN_SIZE, 256, 256);

            // 粘贴按钮
            int pasteBtnX = panelX + PASTE_BTN_X;
            boolean pasteHover = isHovered(mouseX, mouseY, pasteBtnX, rowY + 3, BTN_SIZE, BTN_SIZE);
            graphics.blit(PANEL_TEXTURE, pasteBtnX, rowY + 3,
                pasteHover ? PASTE_BTN_HOVER_U : PASTE_BTN_U,
                pasteHover ? PASTE_BTN_HOVER_V : PASTE_BTN_V,
                BTN_SIZE, BTN_SIZE, 256, 256);
        }
    }

    // ==================== 回退渲染路径 (fill) ====================
    private void renderFallback(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;

        // 面板背景
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC333333);

        // 标题
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

            int copyBtnX = panelX + COPY_BTN_X;
            drawButton(graphics, copyBtnX, rowY + 3, "C",
                isHovered(mouseX, mouseY, copyBtnX, rowY + 3, BTN_SIZE, BTN_SIZE),
                true);

            int pasteBtnX = panelX + PASTE_BTN_X;
            drawButton(graphics, pasteBtnX, rowY + 3, "P",
                isHovered(mouseX, mouseY, pasteBtnX, rowY + 3, BTN_SIZE, BTN_SIZE),
                true);
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

        for (int row = 0; row < FrequencyPresetData.PRESET_COUNT; row++) {
            if (copyBtnBounds.get(row).contains(mx, my)) {
                PacketDistributor.sendToServer(new CopyToPresetPayload(linkPos, row));
                return true;
            }
            if (pasteBtnBounds.get(row).contains(mx, my)) {
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

    private void drawButton(GuiGraphics graphics, int x, int y, String label, boolean hovered) {
        drawButton(graphics, x, y, label, hovered, false);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, String label, boolean hovered, boolean bright) {
        // Brighter colors so buttons are clearly visible
        int bgColor = hovered ? 0xFF4488FF : (bright ? 0xFF3366CC : 0xFF444444);
        int borderColor = hovered ? 0xFFAACCFF : (bright ? 0xFF6699FF : 0xFF666666);
        int textColor = hovered ? 0xFFFFFFFF : (bright ? 0xFFFFEE88 : 0xFFCCCCCC);
        graphics.fill(x - 1, y - 1, x + BTN_SIZE + 1, y + BTN_SIZE + 1, borderColor);
        graphics.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, bgColor);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, label, x + 2, y + 1, textColor, false);
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    // ==================== 获取面板边界（用于JEI） ====================
    public Rect2i getBounds() {
        return new Rect2i(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
    }
}