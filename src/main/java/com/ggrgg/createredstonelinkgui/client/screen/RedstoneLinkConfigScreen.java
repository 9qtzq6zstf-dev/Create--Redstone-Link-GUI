package com.ggrgg.createredstonelinkgui.client.screen;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkModeTogglePayload;

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
    private Rect2i toggleButtonBounds;

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
        // Toggle button at bottom-left of the gray canvas area
        this.toggleButtonBounds = new Rect2i(this.leftPos + 10, this.topPos + 58, 20, 20);
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

        // Mode toggle button — only for RedstoneLinkBlock
        if (this.menu.isRedstoneLink()) {
            int bx = this.toggleButtonBounds.getX();
            int by = this.toggleButtonBounds.getY();
            int bw = this.toggleButtonBounds.getWidth();
            int bh = this.toggleButtonBounds.getHeight();

            // Read block state live from client level each frame for real-time visual updates
            boolean isReceiver = false;
            var level = this.minecraft.level;
            if (level != null) {
                var state = level.getBlockState(this.menu.getPos());
                if (state.getBlock() instanceof RedstoneLinkBlock) {
                    isReceiver = state.getValue(RedstoneLinkBlock.RECEIVER);
                }
            }

            if (isReceiver) {
                // Receiver mode: red fill (matching slot 1 color)
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF7D2D3B);
            } else {
                // Transmitter mode: blue fill (matching slot 2 color)
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF5059AB);
            }
            // 2px border around the button
            graphics.fill(bx, by, bx + bw, by + 2, 0xFF222222);
            graphics.fill(bx, by + bh - 2, bx + bw, by + bh, 0xFF222222);
            graphics.fill(bx, by, bx + 2, by + bh, 0xFF222222);
            graphics.fill(bx + bw - 2, by, bx + bw, by + bh, 0xFF222222);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // No labels — all text removed
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.menu.isRedstoneLink() && button == 0) {
            int bx = this.toggleButtonBounds.getX();
            int by = this.toggleButtonBounds.getY();
            int bw = this.toggleButtonBounds.getWidth();
            int bh = this.toggleButtonBounds.getHeight();
            if (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh) {
                // Send toggle packet to server
                PacketDistributor.sendToServer(new RedstoneLinkModeTogglePayload(this.menu.getPos()));

                // Immediately toggle the client-side block state for zero-latency visual feedback.
                // The server will send the authoritative state shortly after.
                var level = this.minecraft.level;
                if (level != null) {
                    var state = level.getBlockState(this.menu.getPos());
                    if (state.getBlock() instanceof RedstoneLinkBlock) {
                        level.setBlock(this.menu.getPos(), state.cycle(RedstoneLinkBlock.RECEIVER), 0);
                    }
                }

                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
