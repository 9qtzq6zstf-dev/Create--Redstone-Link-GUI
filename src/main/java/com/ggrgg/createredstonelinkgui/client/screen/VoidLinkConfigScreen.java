package com.ggrgg.createredstonelinkgui.client.screen;

import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.VoidLinkClaimPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.network.PacketDistributor;

public class VoidLinkConfigScreen extends AbstractLinkConfigScreen<VoidLinkMenu> {

    private static final ResourceLocation OVERLAY_TEXTURE =
            ResourceLocation.parse("createredstonelinkgui:textures/void_link.png");

    public VoidLinkConfigScreen(VoidLinkMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
    }

    @Override
    protected ResourceLocation getOverlayTexture() {
        return OVERLAY_TEXTURE;
    }

    @Override
    protected BlockPos getBlockPos() {
        return this.menu.getPos();
    }

    @Override
    protected Object getBehaviour() {
        return this.menu.getBehaviour();
    }

    @Override
    protected void applyFrequencyChange(int slotIndex, boolean isFirst, ItemStack stack) {
        Object behaviour = getBehaviour();
        if (behaviour != null) {
            FrequencyHelper.applyFrequencyChangeDirect(behaviour, isFirst, stack);
        }
    }

    @Override
    protected int getBlockPreviewX() {
        return 225;
    }

    @Override
    protected int getBlockPreviewY() {
        return 48;
    }

    @Override
    protected void addExtraWidgets(int contentLeft, int contentTop) {
        // Claim skull button
        SkullButton skullBtn = new SkullButton(contentLeft + 79, contentTop + 64, btn -> {
            Object b = getBehaviour();
            if (b != null) {
                var owner = VoidLinkHelper.getOwner(b);
                if (owner == null || (this.minecraft.player != null && owner.getId().equals(this.minecraft.player.getUUID()))) {
                    PacketDistributor.sendToServer(new VoidLinkClaimPayload(getBlockPos()));
                }
            }
        });
        this.addRenderableWidget(skullBtn);
    }

    private class SkullButton extends Button {
        private ItemStack cachedStack;
        private java.util.UUID lastOwnerId;

        public SkullButton(int x, int y, Button.OnPress onPress) {
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
}