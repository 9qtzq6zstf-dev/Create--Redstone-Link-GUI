package com.ggrgg.createredstonelinkgui.client.screen;

import com.ggrgg.createredstonelinkgui.client.RedstoneLinkMoveHandler;
import com.ggrgg.createredstonelinkgui.client.screen.widget.RedstoneLinkToggleWidget;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
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

    // ==================== 覆盖层纹理偏移（有效区域起始） ====================
    private static final int UV_OFFSET_X = 16;
    private static final int UV_OFFSET_Y = 160;

    // ==================== 槽位纹理坐标（相对于整个 256x256 纹理） ====================
    private static final int SLOT1_UV_X = 77;
    private static final int SLOT1_UV_Y = 188;
    private static final int SLOT2_UV_X = 113;
    private static final int SLOT2_UV_Y = 188;
    private static final int SLOT_SIZE = 16;

    // ==================== Move 按钮纹理坐标 ====================
    private static final int MOVE_UV_X = 26;
    private static final int MOVE_UV_Y = 223;
    private static final int MOVE_ICON_SIZE = 18;

    // ==================== 返回按钮纹理坐标 ====================
    private static final int BACK_UV_X = 165;
    private static final int BACK_UV_Y = 223;
    private static final int BACK_ICON_SIZE = 18;

    // ==================== 槽位边界（用于 JEI/EMI 拖拽） ====================
    public Rect2i slot1Bounds;
    public Rect2i slot2Bounds;

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

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int contentLeft = x + (this.imageWidth - OVERLAY_WIDTH) / 2;
        int contentTop = y + CONTENT_TOP_OFFSET;

        // === 计算槽位屏幕位置 ===
        this.slot1Bounds = new Rect2i(
            contentLeft + (SLOT1_UV_X - UV_OFFSET_X),
            contentTop + (SLOT1_UV_Y - UV_OFFSET_Y),
            SLOT_SIZE,
            SLOT_SIZE
        );
        this.slot2Bounds = new Rect2i(
            contentLeft + (SLOT2_UV_X - UV_OFFSET_X),
            contentTop + (SLOT2_UV_Y - UV_OFFSET_Y),
            SLOT_SIZE,
            SLOT_SIZE
        );

        // === SR 切换按钮（原始材质 RedstoneLinkToggleWidget） ===
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

        // === Move 按钮（自定义 ImageButton） ===
        ImageButton moveButton = new ImageButton(
            contentLeft + 10, contentTop + 63,
            MOVE_ICON_SIZE, MOVE_ICON_SIZE,
            OVERLAY_TEXTURE,
            MOVE_UV_X, MOVE_UV_Y,
            (btn) -> {
                RedstoneLinkMoveHandler.startRelocating(this.menu.getPos());
                this.minecraft.setScreen(null);
            }
        );
        this.addRenderableWidget(moveButton);

        // === 返回按钮（自定义 ImageButton） ===
        ImageButton backButton = new ImageButton(
            contentLeft + 149, contentTop + 63,
            BACK_ICON_SIZE, BACK_ICON_SIZE,
            OVERLAY_TEXTURE,
            BACK_UV_X, BACK_UV_Y,
            (btn) -> {
                this.minecraft.setScreen(null);  // 关闭当前界面，返回游戏
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

    // ==================== 渲染 ====================
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    // ==================== 背景绘制 ====================
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int contentLeft = x + (this.imageWidth - OVERLAY_WIDTH) / 2;
        int contentTop = y + CONTENT_TOP_OFFSET;

        // 1. 自定义覆盖层（从纹理 (16,160) 开始截取 181x88）
        graphics.blit(OVERLAY_TEXTURE, contentLeft, contentTop, UV_OFFSET_X, UV_OFFSET_Y, OVERLAY_WIDTH, OVERLAY_HEIGHT, 256, 256);

        // 2. 玩家背包（居中）
        int backpackX = x + (this.imageWidth - BACKPACK_WIDTH) / 2;
        int backpackY = y + BACKPACK_TOP_OFFSET;
        graphics.blit(BASE_TEXTURE, backpackX, backpackY, 0, 0, BACKPACK_WIDTH, BACKPACK_HEIGHT, 256, 256);
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

        public ImageButton(int x, int y, int width, int height, ResourceLocation texture, int u, int v, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.texWidth = 256;
            this.texHeight = 256;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 绘制纹理
            graphics.blit(texture, getX(), getY(), u, v, width, height, texWidth, texHeight);
            // 悬停时叠加半透明白色，实现变浅效果
            if (isHovered()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x80FFFFFF);
            }
        }
    }
}