package com.ggrgg.createredstonelinkgui.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.ggrgg.createredstonelinkgui.client.RedstoneLinkMoveHandler;
import com.ggrgg.createredstonelinkgui.client.screen.widget.FrequencyPresetPanel;
import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;
import com.ggrgg.createredstonelinkgui.compat.frequency.SymbolPickerScreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Abstract base for redstone link and void link config screens.
 * Consolidates ~80% shared code between RedstoneLinkConfigScreen and VoidLinkConfigScreen.
 */
public abstract class AbstractLinkConfigScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    // ==================== 共享纹理 ====================
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.parse("create:textures/gui/player_inventory.png");

    // ==================== 尺寸常量 ====================
    protected static final int OVERLAY_WIDTH = 181;
    protected static final int OVERLAY_HEIGHT = 88;
    protected static final int BACKPACK_WIDTH = 175;
    protected static final int BACKPACK_HEIGHT = 108;
    protected static final int CONTENT_TOP_OFFSET = 6;
    protected static final int BACKPACK_TOP_OFFSET = 94;

    // ==================== 覆盖层纹理偏移 ====================
    protected static final int UV_OFFSET_X = 16;
    protected static final int UV_OFFSET_Y = 160;

    // ==================== 槽位纹理坐标 ====================
    protected static final int SLOT1_UV_X = 77;
    protected static final int SLOT1_UV_Y = 188;
    protected static final int SLOT2_UV_X = 113;
    protected static final int SLOT2_UV_Y = 188;
    protected static final int SLOT_SIZE = 16;

    // ==================== 按钮纹理坐标 ====================
    protected static final int MOVE_UV_X = 26;
    protected static final int MOVE_UV_Y = 223;
    protected static final int BACK_UV_X = 165;
    protected static final int BACK_UV_Y = 223;
    protected static final int ICON_SIZE = 18;

    // ==================== 标题位置偏移 ====================
    private static final int TITLE_Y_OFFSET = 4;

    // ==================== 槽位边界 ====================
    public Rect2i slot1Bounds;
    public Rect2i slot2Bounds;
    public Rect2i blockPreviewBounds;

    // ==================== 预设面板 ====================
    private FrequencyPresetPanel presetPanel;
    public Rect2i presetPanelBounds;

    // ==================== 构造函数 ====================
    public AbstractLinkConfigScreen(T menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = CONTENT_TOP_OFFSET + OVERLAY_HEIGHT + BACKPACK_HEIGHT + 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // ==================== 子类钩子 ====================
    /** The overlay texture for this link type (redstone_link.png or void_link.png). */
    protected abstract ResourceLocation getOverlayTexture();

    /** The block position of the link block. */
    protected abstract BlockPos getBlockPos();

    /** The block behaviour object (LinkBehaviour or VoidLinkBehaviour). */
    protected abstract Object getBehaviour();

    /**
     * Apply a frequency change on the server side.
     * Calls RedstoneLinkMenu.applyFrequencyChangeDirect or VoidLinkMenu.applyFrequencyChangeDirect.
     */
    protected abstract void applyFrequencyChange(int slotIndex, boolean isFirst, ItemStack stack);

    /** Block preview X offset relative to screen left. */
    protected abstract int getBlockPreviewX();

    /** Block preview Y offset relative to content top. */
    protected abstract int getBlockPreviewY();

    /**
     * Hook for subclasses to add extra widgets (toggle, skull button, etc.).
     * Called at the end of init().
     */
    protected void addExtraWidgets(int contentLeft, int contentTop) {}

    // ==================== 初始化 ====================
    @Override
    protected void init() {
        super.init();

        int leftPos = (this.width - this.imageWidth) / 2;
        int contentLeft = leftPos + (this.imageWidth - OVERLAY_WIDTH) / 2 + 3;
        int contentTop = (this.height - this.imageHeight) / 2 + CONTENT_TOP_OFFSET;

        // 槽位边界 (original positions - unchanged)
        this.slot1Bounds = new Rect2i(
            leftPos + 101,
            contentTop + (SLOT1_UV_Y - UV_OFFSET_Y),
            SLOT_SIZE,
            SLOT_SIZE
        );
        this.slot2Bounds = new Rect2i(
            leftPos + 137,
            contentTop + (SLOT2_UV_Y - UV_OFFSET_Y),
            SLOT_SIZE,
            SLOT_SIZE
        );
        this.blockPreviewBounds = new Rect2i(leftPos + 215, contentTop + 30, 64, 64);

        // === 预设面板（浮动在左侧外部） ===
        int panelX = leftPos - FrequencyPresetPanel.PANEL_WIDTH - 10;
        int panelY = contentTop + 2;
        FrequencyPresetData presetData = FrequencyPresetData.get(this.minecraft.player);
        this.presetPanel = new FrequencyPresetPanel(panelX, panelY, getBlockPos(), presetData);
        this.presetPanelBounds = new Rect2i(panelX, panelY,
            FrequencyPresetPanel.PANEL_WIDTH, FrequencyPresetPanel.PANEL_HEIGHT);

        // === Move 按钮 ===
        ImageButton moveButton = new ImageButton(
            contentLeft + 10, contentTop + 63,
            ICON_SIZE, ICON_SIZE,
            getOverlayTexture(),
            MOVE_UV_X, MOVE_UV_Y,
            Component.translatable("gui.createredstonelinkgui.relocate"),
            (btn) -> {
                RedstoneLinkMoveHandler.startRelocating(getBlockPos());
                this.minecraft.setScreen(null);
            }
        );
        this.addRenderableWidget(moveButton);

        // === 返回按钮 ===
        ImageButton backButton = new ImageButton(
            contentLeft + 149, contentTop + 63,
            ICON_SIZE, ICON_SIZE,
            getOverlayTexture(),
            BACK_UV_X, BACK_UV_Y,
            Component.translatable("gui.createredstonelinkgui.close"),
            (btn) -> this.minecraft.setScreen(null)
        );
        this.addRenderableWidget(backButton);

        // 子类钩子
        addExtraWidgets(contentLeft, contentTop);
    }

    // ==================== 频率更新 ====================
    public void updateFrequencySlot(int slotIndex, ItemStack stack) {
        Object behaviour = getBehaviour();
        if (behaviour != null) {
            applyFrequencyChange(slotIndex, slotIndex == 0, stack);
        }
        com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload payload =
            new com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload(
                getBlockPos(), stack, slotIndex);
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
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

    private int hitTestFrequencySlot(double mouseX, double mouseY) {
        if (slot1Bounds != null && slot1Bounds.contains((int) mouseX, (int) mouseY)) return 0;
        if (slot2Bounds != null && slot2Bounds.contains((int) mouseX, (int) mouseY)) return 1;
        return -1;
    }

    // ==================== 输入处理 ====================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check preset panel first
        if (presetPanel != null && presetPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 2) {
            int slot = hitTestFrequencySlot(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack current = this.menu.getSlot(slot).getItem();
                if (isFrequencySymbol(current)) {
                    Minecraft.getInstance().setScreen(new SymbolPickerScreen(getBlockPos(), slot));
                    return true;
                }
                // Consume middle-click even for non-symbol items to
                // prevent it from falling through to menu.clicked() (button 2 → left-click behavior)
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ==================== 跳过预设槽位渲染 ====================
    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        // Skip preset slots (slots 2-9) — the preset panel handles their rendering
        if (slot.getContainerSlot() >= 2 && slot.getContainerSlot() < 10) {
            return;
        }
        super.renderSlot(graphics, slot);
    }

    // ==================== 渲染 ====================
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        // ========== 预设面板渲染（浮动在左侧外部） ==========
        if (presetPanel != null) {
            presetPanel.render(graphics, mouseX, mouseY, partialTick);
        }

        // ========== 频率槽位工具提示 ==========
        if (this.slot1Bounds != null && this.slot1Bounds.contains(mouseX, mouseY)) {
            Slot slot = this.menu.getSlot(0);
            int lineCount = 1 + (isFrequencySymbol(slot.getItem()) ? 1 : 0);
            int yOffset = -20 - (lineCount - 1) * this.minecraft.font.lineHeight;
            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(Component.translatable("gui.createredstonelinkgui.frequency_first")
                    .withStyle(ChatFormatting.BLUE));
            if (isFrequencySymbol(slot.getItem())) {
                tooltipLines.add(Component.translatable("gui.createredstonelinkgui.middle_click_swap")
                        .withStyle(ChatFormatting.GOLD));
            }
            graphics.renderTooltip(this.minecraft.font, tooltipLines, java.util.Optional.empty(),
                    mouseX, mouseY + yOffset);
        } else if (this.slot2Bounds != null && this.slot2Bounds.contains(mouseX, mouseY)) {
            Slot slot = this.menu.getSlot(1);
            int lineCount = 1 + (isFrequencySymbol(slot.getItem()) ? 1 : 0);
            int yOffset = -20 - (lineCount - 1) * this.minecraft.font.lineHeight;
            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(Component.translatable("gui.createredstonelinkgui.frequency_second")
                    .withStyle(ChatFormatting.BLUE));
            if (isFrequencySymbol(slot.getItem())) {
                tooltipLines.add(Component.translatable("gui.createredstonelinkgui.middle_click_swap")
                        .withStyle(ChatFormatting.GOLD));
            }
            graphics.renderTooltip(this.minecraft.font, tooltipLines, java.util.Optional.empty(),
                    mouseX, mouseY + yOffset);
        } else if (presetPanel != null) {
            // Preset panel tooltips
            presetPanel.renderTooltips(graphics, mouseX, mouseY);
        }
        // ===================================

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    // ==================== 背景绘制 ====================
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int contentLeft = x + (this.imageWidth - OVERLAY_WIDTH) / 2 + 3;
        int contentTop = y + CONTENT_TOP_OFFSET;

        // 绘制覆盖层
        graphics.blit(getOverlayTexture(), contentLeft, contentTop, UV_OFFSET_X, UV_OFFSET_Y,
                OVERLAY_WIDTH, OVERLAY_HEIGHT, 256, 256);

        // 绘制背包
        int backpackX = x + (this.imageWidth - BACKPACK_WIDTH) / 2;
        int backpackY = y + BACKPACK_TOP_OFFSET;
        graphics.blit(BASE_TEXTURE, backpackX, backpackY, 0, 0, BACKPACK_WIDTH, BACKPACK_HEIGHT, 256, 256);

        // 标题
        Font font = this.minecraft.font;
        Component titleText = Component.translatable("gui.createredstonelinkgui.frequencies_settings");
        int titleWidth = font.width(titleText);
        int titleX = contentLeft + (OVERLAY_WIDTH - titleWidth) / 2;
        int titleY = contentTop + TITLE_Y_OFFSET;
        graphics.drawString(font, titleText, titleX, titleY, 0xFF3C3B47, false);

        // 方块预览
        if (this.minecraft.level != null) {
            var blockState = this.minecraft.level.getBlockState(getBlockPos());
            var blockEntity = this.minecraft.level.getBlockEntity(getBlockPos());
            BlockPreviewRenderer.render(graphics, blockState, blockEntity,
                    x + getBlockPreviewX(), contentTop + getBlockPreviewY());
        }
    }

    // ==================== 隐藏原版标签 ====================
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    // ==================== 自定义 ImageButton 类 ====================
    protected static class ImageButton extends Button {
        private final ResourceLocation texture;
        private final int u, v;
        private final int texWidth, texHeight;
        private final Component tooltip;
        private static final int HOVER_COLOR = 0x22_1500FF;

        public ImageButton(int x, int y, int width, int height, ResourceLocation texture,
                           int u, int v, Component tooltip, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.texWidth = 256;
            this.texHeight = 256;
            this.tooltip = tooltip;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(texture, getX(), getY(), u, v, width, height, texWidth, texHeight);
            if (isHovered()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, HOVER_COLOR);
                if (tooltip != null) {
                    Font font = net.minecraft.client.Minecraft.getInstance().font;
                    graphics.renderTooltip(font, tooltip, mouseX, mouseY);
                }
            }
        }
    }
}