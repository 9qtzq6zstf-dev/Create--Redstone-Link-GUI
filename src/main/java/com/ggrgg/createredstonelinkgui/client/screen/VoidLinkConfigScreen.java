package com.ggrgg.createredstonelinkgui.client.screen;

import com.ggrgg.createredstonelinkgui.client.RedstoneLinkMoveHandler;
import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.ggrgg.createredstonelinkgui.common.network.VoidLinkClaimPayload;
import com.ggrgg.createredstonelinkgui.compat.frequency.SymbolPickerOverlay;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.network.PacketDistributor;

public class VoidLinkConfigScreen extends AbstractContainerScreen<VoidLinkMenu> {

    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.parse("create:textures/gui/player_inventory.png");
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.parse("createredstonelinkgui:textures/void_link.png");

    private static final int OVERLAY_WIDTH = 181;
    private static final int OVERLAY_HEIGHT = 88;
    private static final int BACKPACK_WIDTH = 175;
    private static final int BACKPACK_HEIGHT = 108;
    private static final int CONTENT_TOP_OFFSET = 6;
    private static final int BACKPACK_TOP_OFFSET = 94;

    private static final int UV_OFFSET_X = 16;
    private static final int UV_OFFSET_Y = 160;

    private static final int SLOT1_UV_X = 77;
    private static final int SLOT1_UV_Y = 188;
    private static final int SLOT2_UV_X = 113;
    private static final int SLOT2_UV_Y = 188;
    private static final int SLOT_SIZE = 16;

    private static final int MOVE_UV_X = 26;
    private static final int MOVE_UV_Y = 223;
    private static final int BACK_UV_X = 165;
    private static final int BACK_UV_Y = 223;
    private static final int ICON_SIZE = 18;

    private static final int TITLE_Y_OFFSET = 4;

    public Rect2i slot1Bounds;
    public Rect2i slot2Bounds;
    public Rect2i blockPreviewBounds;

    // ==================== 频率符号选择器覆盖层 ====================
    private SymbolPickerOverlay symbolPicker;

    public VoidLinkConfigScreen(VoidLinkMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = CONTENT_TOP_OFFSET + OVERLAY_HEIGHT + BACKPACK_HEIGHT + 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = (this.width - this.imageWidth) / 2;
        int contentLeft = leftPos + (this.imageWidth - OVERLAY_WIDTH) / 2 + 3;
        int contentTop = (this.height - this.imageHeight) / 2 + CONTENT_TOP_OFFSET;

        this.slot1Bounds = new Rect2i(
            leftPos + 101,
            contentTop + (SLOT1_UV_Y - UV_OFFSET_Y),
            SLOT_SIZE, SLOT_SIZE
        );
        this.slot2Bounds = new Rect2i(
            leftPos + 137,
            contentTop + (SLOT2_UV_Y - UV_OFFSET_Y),
            SLOT_SIZE, SLOT_SIZE
        );
        this.blockPreviewBounds = new Rect2i(leftPos + 215, contentTop + 30, 64, 64);

        // Claim skull button — renders player head or skeleton skull
        SkullButton skullBtn = new SkullButton(contentLeft + 79, contentTop + 64, btn -> {
            Object b = this.menu.getBehaviour();
            if (b != null) {
                var owner = VoidLinkHelper.getOwner(b);
                if (owner == null || (this.minecraft.player != null && owner.getId().equals(this.minecraft.player.getUUID()))) {
                    PacketDistributor.sendToServer(new VoidLinkClaimPayload(this.menu.getPos()));
                }
            }
        });
        this.addRenderableWidget(skullBtn);

        // Move button
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

        // Close button
        ImageButton backButton = new ImageButton(
            contentLeft + 149, contentTop + 63,
            ICON_SIZE, ICON_SIZE,
            OVERLAY_TEXTURE,
            BACK_UV_X, BACK_UV_Y,
            Component.translatable("gui.createredstonelinkgui.close"),
            (btn) -> this.minecraft.setScreen(null)
        );
        this.addRenderableWidget(backButton);
    }

    public void updateFrequencySlot(int slotIndex, ItemStack stack) {
        if (this.menu instanceof VoidLinkMenu vm) {
            var behaviour = vm.getBehaviour();
            if (behaviour != null) {
                VoidLinkMenu.applyFrequencyChangeDirect(behaviour, slotIndex == 0, stack);
            }
            PacketDistributor.sendToServer(new RedstoneLinkFrequencyPayload(vm.getPos(), stack, slotIndex));
        }
    }

    // ==================== 频率符号检测 ====================
    /**
     * Checks if the given ItemStack is a frequency mod symbol (excluding symbol_frame and symbol_empty).
     * Uses only BuiltInRegistries — no frequency-mod imports.
     */
    private static boolean isFrequencySymbol(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("frequency")) return false;
        String path = id.getPath();
        if (!path.startsWith("symbol_")) return false;
        if (path.equals("symbol_frame")) return false;
        if (path.equals("symbol_empty")) return false;
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
        if (symbolPicker != null) {
            if (symbolPicker.isMouseOver(mouseX, mouseY)) {
                if (symbolPicker.mouseClicked(mouseX, mouseY, button)) {
                    symbolPicker = null; // symbol was picked, close overlay
                    return true;
                }
                return true;
            } else {
                symbolPicker = null;
                return true;
            }
        }

        // Middle-click on frequency slot with frequency symbol → open picker
        if (button == 2) {
            int slot = hitTestFrequencySlot(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack current = this.menu.getSlot(slot).getItem();
                if (isFrequencySymbol(current)) {
                    symbolPicker = new SymbolPickerOverlay(
                        (idx, stack) -> updateFrequencySlot(idx, stack),
                        slot,
                        this.font
                    );
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (symbolPicker != null) {
            symbolPicker = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (symbolPicker == null) {
            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);

            // ========== 频率槽位工具提示（同 RedstoneLinkConfigScreen） ==========
            if (this.slot1Bounds != null && this.slot1Bounds.contains(mouseX, mouseY)) {
                Slot slot = this.menu.getSlot(0);
                int yOffset = slot.hasItem() ? -20 : 0;
                graphics.renderTooltip(this.minecraft.font,
                        Component.translatable("gui.createredstonelinkgui.frequency_first")
                                .withStyle(ChatFormatting.BLUE),
                        mouseX, mouseY + yOffset);
            } else if (this.slot2Bounds != null && this.slot2Bounds.contains(mouseX, mouseY)) {
                Slot slot = this.menu.getSlot(1);
                int yOffset = slot.hasItem() ? -20 : 0;
                graphics.renderTooltip(this.minecraft.font,
                        Component.translatable("gui.createredstonelinkgui.frequency_second")
                                .withStyle(ChatFormatting.BLUE),
                        mouseX, mouseY + yOffset);
            }
            // ==============================================================

            this.renderTooltip(graphics, mouseX, mouseY);
        } else {
            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);
            symbolPicker.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int contentLeft = x + (this.imageWidth - OVERLAY_WIDTH) / 2 + 3;
        int contentTop = y + CONTENT_TOP_OFFSET;

        graphics.blit(OVERLAY_TEXTURE, contentLeft, contentTop, UV_OFFSET_X, UV_OFFSET_Y, OVERLAY_WIDTH, OVERLAY_HEIGHT, 256, 256);

        int backpackX = x + (this.imageWidth - BACKPACK_WIDTH) / 2;
        int backpackY = y + BACKPACK_TOP_OFFSET;
        graphics.blit(BASE_TEXTURE, backpackX, backpackY, 0, 0, BACKPACK_WIDTH, BACKPACK_HEIGHT, 256, 256);

        // Title
        Font font = this.minecraft.font;
        Component titleText = Component.translatable("gui.createredstonelinkgui.frequencies_settings");
        int titleWidth = font.width(titleText);
        int titleX = contentLeft + (OVERLAY_WIDTH - titleWidth) / 2;
        int titleY = contentTop + TITLE_Y_OFFSET;
        graphics.drawString(font, titleText, titleX, titleY, 0xFF3C3B47, false);

        // Block preview
        if (this.minecraft.level != null) {
            var blockState = this.minecraft.level.getBlockState(this.menu.getPos());
            var blockEntity = this.minecraft.level.getBlockEntity(this.menu.getPos());
            BlockPreviewRenderer.render(graphics, blockState, blockEntity, x + 225, contentTop + 48);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    private class SkullButton extends Button {
        private ItemStack cachedStack;
        private java.util.UUID lastOwnerId;

        public SkullButton(int x, int y, OnPress onPress) {
            super(x, y, ICON_SIZE, ICON_SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
            this.cachedStack = new ItemStack(Items.SKELETON_SKULL);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Object b = menu.getBehaviour();
            if (b != null) {
                var owner = VoidLinkHelper.getOwner(b);
                java.util.UUID currentId = (owner != null) ? owner.getId() : null;
                if (!java.util.Objects.equals(currentId, lastOwnerId)) {
                    lastOwnerId = currentId;
                    if (owner != null) {
                        cachedStack = new ItemStack(Items.PLAYER_HEAD);
                        cachedStack.set(DataComponents.PROFILE, new ResolvableProfile(owner));
                    } else {
                        cachedStack = new ItemStack(Items.SKELETON_SKULL);
                    }
                }
            }
            graphics.renderItem(cachedStack, getX(), getY());

            // 工具提示（悬停时显示）
            if (isHovered()) {
                boolean owned = false;
                Object behaviour = menu.getBehaviour();
                if (behaviour != null) {
                    var owner = VoidLinkHelper.getOwner(behaviour);
                    owned = (owner != null);
                }
                Component tooltip = owned ?
                        Component.translatable("gui.createredstonelinkgui.forfeit") :
                        Component.translatable("gui.createredstonelinkgui.own");
                graphics.renderTooltip(minecraft.font, tooltip, mouseX, mouseY);
            }
        }
    }

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