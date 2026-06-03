package com.ggrgg.createredstonelinkgui.client.screen.widget;

import java.util.List;
import java.util.function.BooleanSupplier;

import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkModeTogglePayload;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A one-click toggle switch widget following Create's widget pattern.
 * Reads the receiver state live each frame via BooleanSupplier for zero-latency visual feedback.
 */
public class RedstoneLinkToggleWidget extends AbstractSimiWidget {

    private static final int TRACK_WIDTH = 18;
    private static final int TRACK_HEIGHT = 4;
    private static final int KNOB_SIZE = 10;

    private final BlockPos pos;
    private final BooleanSupplier isReceiver;
    private Font font;

    public RedstoneLinkToggleWidget(int x, int y, BlockPos pos, BooleanSupplier isReceiver) {
        super(x, y, 54, 16);
        this.pos = pos;
        this.isReceiver = isReceiver;
    }

    @Override
    public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        if (font == null)
            font = net.minecraft.client.Minecraft.getInstance().font;

        boolean recv = isReceiver.getAsBoolean();

        // Update tooltip based on current state
        toolTip = List.of(Component.literal(recv ? "Receive" : "Send"));

        // === "S" label (left) ===
        graphics.drawString(font, "S", getX() + 3, getY() + 4, 0xFFFFFFFF);

        // === Track (centered vertically) ===
        int trackX = getX() + 13;
        int trackY = getY() + 6;
        graphics.fill(trackX, trackY, trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT, 0xFF555555);
        graphics.fill(trackX, trackY, trackX + TRACK_WIDTH, trackY + 1, 0xFF777777);

        // === Knob (square) ===
        int knobX = recv ? trackX + TRACK_WIDTH - KNOB_SIZE : trackX;
        int knobY = trackY - 3;

        // Knob shadow
        graphics.fill(knobX + 1, knobY + 1, knobX + KNOB_SIZE + 1, knobY + KNOB_SIZE + 1, 0xFF333333);
        // Knob face
        graphics.fill(knobX, knobY, knobX + KNOB_SIZE, knobY + KNOB_SIZE, 0xFFC6C6C6);
        // Knob top-left highlight
        graphics.fill(knobX, knobY, knobX + KNOB_SIZE, knobY + 1, 0xFFE8E8E8);
        graphics.fill(knobX, knobY, knobX + 1, knobY + KNOB_SIZE, 0xFFE8E8E8);
        // Knob bottom-right shadow
        graphics.fill(knobX, knobY + KNOB_SIZE - 1, knobX + KNOB_SIZE, knobY + KNOB_SIZE, 0xFF888888);
        graphics.fill(knobX + KNOB_SIZE - 1, knobY, knobX + KNOB_SIZE, knobY + KNOB_SIZE, 0xFF888888);

        // === "R" label (right) ===
        graphics.drawString(font, "R", getX() + 43, getY() + 4, 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // Send toggle packet to server
        PacketDistributor.sendToServer(new RedstoneLinkModeTogglePayload(pos));

        // Immediately toggle client-side block state for zero-latency visual feedback.
        // The server will send the authoritative state shortly after.
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var level = minecraft.level;
        if (level != null) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof RedstoneLinkBlock) {
                level.setBlock(pos, state.cycle(RedstoneLinkBlock.RECEIVER), 0);
            }
        }
    }
}
