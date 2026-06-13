package com.ggrgg.createredstonelinkgui.client;

import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkMovePayload;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class RedstoneLinkMoveHandler {

    private static BlockPos sourcePos;
    private static BlockState sourceState;
    private static boolean active;
    private static BlockPos validTarget;
    private static Direction validFace;

    public static void startRelocating(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        BlockState state = level.getBlockState(pos);
        sourcePos = pos;
        sourceState = state;
        active = true;
        validTarget = null;
        validFace = null;
    }

    public static void clientTick() {
        if (!active || sourcePos == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cancel();
            return;
        }

        // Check distance
        if (!sourcePos.closerThan(mc.player.blockPosition(), 16)) {
            mc.player.displayClientMessage(Component.empty(), true);
            cancel();
            return;
        }

        // Show outline on source block
        Outliner.getInstance()
                .showAABB(sourcePos, new AABB(sourcePos))
                .colored(AnimationTickHolder.getTicks() % 16 > 8 ? 0x38b764 : 0xa7f070)
                .lineWidth(1 / 16f);

        mc.player.displayClientMessage(
                Component.translatable("gui.createredstonelinkgui.click_to_relocate"),
                true);

        // Evaluate potential target
        validTarget = null;
        validFace = null;

        if (!(mc.hitResult instanceof BlockHitResult bhr) || bhr.getType() == Type.MISS)
            return;

        Direction clickedFace = bhr.getDirection();

        // Calculate the target position (the block face the player is looking at)
        Vec3 offsetPos = bhr.getLocation()
                .add(Vec3.atLowerCornerOf(clickedFace.getNormal())
                        .scale(1 / 32f));
        BlockPos pos = BlockPos.containing(offsetPos);

        BlockState targetState = mc.level.getBlockState(pos);

        // If moving to a new position, it must be air or replaceable
        // If moving in place, allow it (orientation change only)
        if (!pos.equals(sourcePos) && !targetState.isAir() && !targetState.canBeReplaced()) return;

        // Dynamically determine the new state via the block's own placement logic
        BlockPlaceContext placeContext = new BlockPlaceContext(mc.level, mc.player, InteractionHand.MAIN_HAND,
                ItemStack.EMPTY, new BlockHitResult(Vec3.atCenterOf(pos), clickedFace, pos, false));
        BlockState newState = sourceState.getBlock().getStateForPlacement(placeContext);
        if (newState == null) return;

        // Validate survivability with the new orientation
        if (!newState.canSurvive(mc.level, pos)) return;

        // Must be within range
        if (!pos.closerThan(sourcePos, 24)) return;

        validTarget = pos;
        validFace = clickedFace;

        // Show ghost outline at target
        Outliner.getInstance()
                .showAABB("target", new AABB(pos))
                .colored(0xeeeeee)
                .disableLineNormals()
                .lineWidth(1 / 16f);
    }

    public static boolean onRightClick() {
        if (!active || sourcePos == null) return false;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player.isShiftKeyDown()) {
            validTarget = null;
            validFace = null;
        }

        if (validTarget != null && validFace != null) {
            // Send move packet to server with clicked face for orientation
            PacketDistributor.sendToServer(new RedstoneLinkMovePayload(sourcePos, validTarget, validFace));

            mc.player.displayClientMessage(
                    Component.translatable("gui.createredstonelinkgui.link_relocated"),
                    true);
        } else {
            mc.player.displayClientMessage(
                    Component.translatable("gui.createredstonelinkgui.relocation_aborted"),
                    true);
        }

        cancel();
        return true;
    }

    public static void cancel() {
        active = false;
        sourcePos = null;
        sourceState = null;
        validTarget = null;
        validFace = null;
    }

    public static boolean isActive() {
        return active;
    }
}