package com.ggrgg.createredstonelinkgui.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.ggrgg.createredstonelinkgui.client.RedstoneLinkMoveHandler;
import com.ggrgg.createredstonelinkgui.client.screen.widget.RedstoneLinkToggleWidget;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.ggrgg.createredstonelinkgui.compat.frequency.SymbolPickerScreen;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class RedstoneLinkConfigScreen extends AbstractContainerScreen<RedstoneLinkMenu> {

    // ==================== 纹理 ====================
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.parse("create:textures/gui/player_inventory.png");
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.parse("createredstonelinkgui:textures/redstone_link.png");

    // ==================== 尺寸常量 ====================
    private static final int OVERLAY_WIDTH = 181;
    private static final int OVERLAY_HEIGHT = 88;
    private static final int BACKPACK_WIDTH = 175;
    private static final int BACKPACK_HEIGHT = 108;
    private static final int CONTENT_TOP_OFFSET = 6;
    private static final int BACKPACK_TOP_OFFSET = 94;

    // ==================== 覆盖层纹理偏移 ====================
    private static final int UV_OFFSET_X = 16;
    private static final int UV_OFFSET_Y = 160;

    // ==================== 槽位纹理坐标 ====================
    private static final int SLOT1_UV_X = 77;
    private static final int SLOT1_UV_Y = 188;
    private static final int SLOT2_UV_X = 113;
    private static final int SLOT2_UV_Y = 188;
    private static final int SLOT_SIZE = 16;

    // ==================== 按钮纹理坐标 ====================
    private static final int MOVE_UV_X = 26;
    private static final int MOVE_UV_Y = 223;
    private static final int BACK_UV_X = 165;
    private static final int BACK_UV_Y = 223;
    private static final int ICON_SIZE = 18;

    // ==================== 标题位置偏移（相对于覆盖层左上角） ====================
    private static final int TITLE_Y_OFFSET = 4; // Y 坐标偏移

    // ==================== 槽位边界 ====================
    public Rect2i slot1Bounds;
    public Rect2i slot2Bounds;
    public Rect2i blockPreviewBounds;

    // ==================== 构造函数 ====================
    public RedstoneLinkConfigScreen(RedstoneLinkMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = CONTENT_TOP_OFFSET + OVERLAY_HEIGHT + BACKPACK_HEIGHT + 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // ==================== 初始化 ====================
    @Override
    protected void init() {
        super.init();

        int leftPos = (this.width - this.imageWidth) / 2;
        int contentLeft = leftPos + (this.imageWidth - OVERLAY_WIDTH) / 2 + 3;
        int contentTop = (this.height - this.imageHeight) / 2 + CONTENT_TOP_OFFSET;

        // 槽位边界
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

        // === SR 切换按钮 ===
        if (this.menu.isRedstoneLink()) {
            this.addRenderableWidget(new RedstoneLinkToggleWidget(
                contentLeft + 65, contentTop + 64,
                this.menu.getPos(),
                () -> {
                    var level = this.minecraft.level;
                    if (level != null) {
                        var state = level.getBlockState(this.menu.getPos());
                        if (state.getBlock() instanceof RedstoneLinkBlock) {
                            return state.getValue(RedstoneLinkBlock.RECEIVER);
                        }
                    }
                    return false;
                }
            ));
        }

        // === Move 按钮 ===
        ImageButton moveButton = new ImageButton(
            contentLeft + 10, contentTop + 63,
            ICON_SIZE, ICON_SIZE,
            OVERLAY_TEXTURE,
            MOVE_UV_X, MOVE_UV_Y,
            Component.translatable("gui.createredstonelinkgui.relocate"),
            (btn) -> {
                RedstoneLinkMoveHandler.startRelocating(this.menu.getPos());
                this.minecraft.setScreen(null);
            }
        );
        this.addRenderableWidget(moveButton);

        // === 返回按钮 ===
        ImageButton backButton = new ImageButton(
            contentLeft + 149, contentTop + 63,
            ICON_SIZE, ICON_SIZE,
            OVERLAY_TEXTURE,
            BACK_UV_X, BACK_UV_Y,
            Component.translatable("gui.createredstonelinkgui.close"),
            (btn) -> {
                this.minecraft.setScreen(null);
            }
        );
        this.addRenderableWidget(backButton);
    }

    // ==================== JEI/EMI 拖拽回调 ====================
    public void updateFrequencySlot(int slotIndex, ItemStack stack) {
        if (this.menu instanceof RedstoneLinkMenu customMenu) {
            var behaviour = customMenu.getBehaviour();
            if (behaviour != null) {
                RedstoneLinkMenu.applyFrequencyChangeDirect(behaviour, slotIndex == 0, stack);
            }
            PacketDistributor.sendToServer(new RedstoneLinkFrequencyPayload(customMenu.getPos(), stack, slotIndex));
        }
    }

    // ==================== 频率符号检测 ====================
    /**
     * Checks if the given ItemStack is a frequency mod symbol (excluding symbol_frame).
     * Uses only BuiltInRegistries — no frequency-mod imports.
     */
    private static boolean isFrequencySymbol(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("frequency")) return false;
        String path = id.getPath();
        if (!path.startsWith("symbol_")) return false;
        if (path.equals("symbol_frame")) return false;
        return true;
    }

    /**
     * Returns the frequency slot index (0 or 1) hit by the given mouse coordinates, or -1 if none.
     */
    private int hitTestFrequencySlot(double mouseX, double mouseY) {
        if (slot1Bounds != null && slot1Bounds.contains((int) mouseX, (int) mouseY)) return 0;
        if (slot2Bounds != null && slot2Bounds.contains((int) mouseX, (int) mouseY)) return 1;
        return -1;
    }

    // ==================== 输入处理 ====================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Middle-click on frequency slot with frequency symbol → open symbol picker
        if (button == 2) {
            int slot = hitTestFrequencySlot(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack current = this.menu.getSlot(slot).getItem();
                if (isFrequencySymbol(current)) {
                    Minecraft.getInstance().setScreen(
                        new SymbolPickerScreen(this.menu.getPos(), slot));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ==================== 渲染 ====================
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        // ========== 频率槽位工具提示 ==========
        if (this.slot1Bounds != null && this.slot1Bounds.contains(mouseX, mouseY)) {
            Slot slot = this.menu.getSlot(0);
            int lineCount = 1 + (isFrequencySymbol(slot.getItem()) ? 1 : 0);
            // Shift tooltip upward to avoid clipping into item tooltip below
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
        graphics.blit(OVERLAY_TEXTURE, contentLeft, contentTop, UV_OFFSET_X, UV_OFFSET_Y, OVERLAY_WIDTH, OVERLAY_HEIGHT, 256, 256);

        // 绘制背包
        int backpackX = x + (this.imageWidth - BACKPACK_WIDTH) / 2;
        int backpackY = y + BACKPACK_TOP_OFFSET;
        graphics.blit(BASE_TEXTURE, backpackX, backpackY, 0, 0, BACKPACK_WIDTH, BACKPACK_HEIGHT, 256, 256);

        // === 绘制标题（居中，使用 drawString） ===
        Font font = this.minecraft.font;
        Component titleText = Component.translatable("gui.createredstonelinkgui.frequencies_settings");
        int titleWidth = font.width(titleText);
        int titleX = contentLeft + (OVERLAY_WIDTH - titleWidth) / 2;
        int titleY = contentTop + TITLE_Y_OFFSET;
        graphics.drawString(font, titleText, titleX, titleY, 0xFF3C3B47, false);

        // === 3D 方块预览 ===
        if (this.minecraft.level != null) {
            var blockState = this.minecraft.level.getBlockState(this.menu.getPos());
            var blockEntity = this.minecraft.level.getBlockEntity(this.menu.getPos());
            BlockPreviewRenderer.render(graphics, blockState, blockEntity, x + 215, contentTop + 30);
        }
    }

    // ==================== 隐藏原版标签 ====================
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不绘制任何标签
    }

    // ==================== 自定义 ImageButton 类 ====================
    private static class ImageButton extends Button {
        private final ResourceLocation texture;
        private final int u, v;
        private final int texWidth, texHeight;
        private final Component tooltip;
        private static final int HOVER_COLOR = 0x22_1500FF;

        public ImageButton(int x, int y, int width, int height, ResourceLocation texture, int u, int v, Component tooltip, OnPress onPress) {
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