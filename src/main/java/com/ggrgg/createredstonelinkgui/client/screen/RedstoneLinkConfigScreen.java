package com.ggrgg.createredstonelinkgui.client.screen;

import com.ggrgg.createredstonelinkgui.client.screen.widget.RedstoneLinkToggleWidget;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class RedstoneLinkConfigScreen extends AbstractLinkConfigScreen<RedstoneLinkMenu> {

    private static final ResourceLocation OVERLAY_TEXTURE =
            ResourceLocation.parse("createredstonelinkgui:textures/redstone_link.png");

    public RedstoneLinkConfigScreen(RedstoneLinkMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
    }

    @Override
    protected ResourceLocation getOverlayTexture() {
        return OVERLAY_TEXTURE;
    }

    @Override
    protected int getBlockPreviewX() {
        return 215;
    }

    @Override
    protected int getBlockPreviewY() {
        return 30;
    }

    @Override
    protected void addExtraWidgets(int contentLeft, int contentTop) {
        // SR 切换按钮
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
    }
}