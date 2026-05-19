package com.ggrgg.createredstonelinkgui.client.screen;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
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

        // Blit customized slot locations onto layout canvas
        graphics.blitSprite(SLOT_SPRITE, this.slot1Bounds.getX(), this.slot1Bounds.getY(), 18, 18);
        graphics.blitSprite(SLOT_SPRITE, this.slot2Bounds.getX(), this.slot2Bounds.getY(), 18, 18);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        
        Component customTitle = Component.literal("Link Frequencies");
        int titleWidth = this.font.width(customTitle);
        graphics.drawString(this.font, customTitle, (this.imageWidth - titleWidth) / 2, 10, 4210752, false);
    }
}
