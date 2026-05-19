package com.ggrgg.createredstonelinkgui.client;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ClientHooks {
    public static void openLinkGui(BlockPos pos, LinkBehaviour behaviour) {
        Minecraft.getInstance().execute(() -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                // FIXED: Using the native vanilla SimpleMenuProvider + custom extra data buffer block
                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new RedstoneLinkMenu(id, inv, pos),
                    Component.literal("Redstone Link Frequency")
                ), buf -> buf.writeBlockPos(pos));
            }
        });
    }
}
