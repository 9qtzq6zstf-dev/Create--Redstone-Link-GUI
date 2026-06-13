package com.ggrgg.createredstonelinkgui.client.screen;

import com.ggrgg.createredstonelinkgui.client.RedstoneLinkMoveHandler;
import com.ggrgg.createredstonelinkgui.client.screen.widget.RedstoneLinkToggleWidget;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class RedstoneLinkConfigScreen extends AbstractContainerScreen<RedstoneLinkMenu> {

    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    public Rect2i slot1Bounds;
    public Rect2i slot2Bounds;

    public RedstoneLinkConfigScreen(RedstoneLinkMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Sets precise pixel alignment bounding limits for JEI drag target calculations
        this.slot1Bounds = new Rect2i(this.leftPos + 53, this.topPos + 25, 18, 18);
        this.slot2Bounds = new Rect2i(this.leftPos + 107, this.topPos + 25, 18, 18);

        // Toggle switch widget — only add for RedstoneLinkBlock
        if (this.menu.isRedstoneLink()) {
            this.addRenderableWidget(new RedstoneLinkToggleWidget(
                this.leftPos + 14, this.topPos + 58,
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

        // "Move this link" button — works for any block with LinkBehaviour
        int x = this.leftPos;
        IconButton relocateButton = new IconButton(x + 14, this.topPos + 80, AllIcons.I_MOVE_GAUGE);
        relocateButton.withCallback(() -> {
            RedstoneLinkMoveHandler.startRelocating(this.menu.getPos());
            this.minecraft.setScreen(null);
        });
        relocateButton.setToolTip(Component.translatable("gui.createredstonelinkgui.relocate"));
        this.addRenderableWidget(relocateButton);
    }

    /**
     * Fired by JEI drag actions. Mutates local behavior for immediate zero-latency feedback
     * while pushing network updates up to the server cluster.
     */
    public void updateFrequencySlot(int slotIndex, ItemStack stack) {
        if (this.menu instanceof RedstoneLinkMenu customMenu) {
            var behaviour = customMenu.getBehaviour();
            if (behaviour != null) {
                // High-Responsiveness: Set immediately on client before server response returns
                RedstoneLinkMenu.applyFrequencyChangeDirect(behaviour, slotIndex == 0, stack);
            }
            PacketDistributor.sendToServer(new RedstoneLinkFrequencyPayload(customMenu.getPos(), stack, slotIndex));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(BASE_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // Draw uniform layout spacing canvas overlay
        graphics.fill(x + 7, y + 7, x + 169, y + 82, 0xFFC6C6C6);

        // First frequency slot — slot sprite first, then 4px border outside the slot
        int s1x = this.slot1Bounds.getX();
        int s1y = this.slot1Bounds.getY();
        graphics.blitSprite(SLOT_SPRITE, s1x, s1y, 18, 18);
        graphics.fill(s1x - 4, s1y - 4, s1x + 22, s1y, 0xFF7D2D3B);       // top
        graphics.fill(s1x - 4, s1y + 18, s1x + 22, s1y + 22, 0xFF7D2D3B); // bottom
        graphics.fill(s1x - 4, s1y, s1x, s1y + 18, 0xFF7D2D3B);           // left
        graphics.fill(s1x + 18, s1y, s1x + 22, s1y + 18, 0xFF7D2D3B);     // right

        // Second frequency slot — slot sprite first, then 4px border outside the slot
        int s2x = this.slot2Bounds.getX();
        int s2y = this.slot2Bounds.getY();
        graphics.blitSprite(SLOT_SPRITE, s2x, s2y, 18, 18);
        graphics.fill(s2x - 4, s2y - 4, s2x + 22, s2y, 0xFF5059AB);       // top
        graphics.fill(s2x - 4, s2y + 18, s2x + 22, s2y + 22, 0xFF5059AB); // bottom
        graphics.fill(s2x - 4, s2y, s2x, s2y + 18, 0xFF5059AB);           // left
        graphics.fill(s2x + 18, s2y, s2x + 22, s2y + 18, 0xFF5059AB);     // right
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // No labels — all text removed
    }
}
