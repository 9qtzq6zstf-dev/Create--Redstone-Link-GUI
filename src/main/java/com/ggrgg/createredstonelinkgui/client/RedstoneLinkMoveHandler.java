package com.ggrgg.createredstonelinkgui.client;

import com.ggrgg.createredstonelinkgui.Config;
import com.ggrgg.createredstonelinkgui.common.SableHelper;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkMovePayload;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class RedstoneLinkMoveHandler {

    private static final double HIGHLIGHT_INSET = 0.0625; // 1/16 block inset from block edge
    private static final double HIGHLIGHT_DEPTH = 0.0625; // 1/16 block slab thickness

    private static BlockPos sourcePos;
    private static BlockState sourceState;
    private static boolean active;
    private static BlockPos validTarget;
    private static Direction validFace;
    private static int moveRange;
    private static String invalidReason;

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
        invalidReason = null;
        moveRange = Config.MOVE_RANGE.get();
    }

    public static void clientTick() {
        if (!active || sourcePos == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cancel();
            return;
        }

        // Show face highlight on source block
        Direction sourceFace = sourceState.hasProperty(BlockStateProperties.FACING)
                ? sourceState.getValue(BlockStateProperties.FACING).getOpposite()
                : Direction.UP;
        Outliner.getInstance()
                .showAABB(sourcePos, getFaceHighlight(sourcePos, sourceFace))
                .colored(AnimationTickHolder.getTicks() % 16 > 8 ? 0x38b764 : 0xa7f070)
                .lineWidth(1 / 16f);

        mc.player.displayClientMessage(
                Component.translatable("gui.createredstonelinkgui.click_to_relocate"),
                true);

        // Evaluate potential target
        validTarget = null;
        validFace = null;
        invalidReason = null;

        if (!(mc.hitResult instanceof BlockHitResult bhr) || bhr.getType() == Type.MISS)
            return;

        Direction clickedFace = bhr.getDirection();
        Direction attachFace = clickedFace.getOpposite();

        // Use the adjacent block position — matches vanilla BlockItem placement behavior
        BlockPos pos = bhr.getBlockPos().relative(clickedFace);
        BlockState targetState = mc.level.getBlockState(pos);
        boolean inPlace = pos.equals(sourcePos);

        // Check: obstructed
        if (!inPlace && !targetState.isAir() && !targetState.canBeReplaced()) {
            invalidReason = "move_fail_obstructed";
            showRedOutline(pos, attachFace);
            return;
        }

        // Check: placement logic
        BlockPlaceContext placeContext = new BlockPlaceContext(mc.level, mc.player, InteractionHand.MAIN_HAND,
                ItemStack.EMPTY, new BlockHitResult(Vec3.atCenterOf(pos), clickedFace, pos, false));
        BlockState newState = sourceState.getBlock().getStateForPlacement(placeContext);
        if (newState == null) {
            invalidReason = "move_fail_no_surface";
            showRedOutline(pos, attachFace);
            return;
        }

        // Check: survivability
        if (!newState.canSurvive(mc.level, pos)) {
            invalidReason = "move_fail_cant_survive";
            showRedOutline(pos, attachFace);
            return;
        }

        // Determine effective range (mirrors server logic)
        int effectiveRange = moveRange;

        var be = mc.level.getBlockEntity(sourcePos);
        if (be != null) {
            var gaugeSupport = BlockEntityBehaviour.get(be, FactoryPanelSupportBehaviour.TYPE);
            if (gaugeSupport != null && !gaugeSupport.getLinkedPanels().isEmpty()) {
                effectiveRange = Math.min(effectiveRange, 24);

                // Check: gauge same-surface constraint
                Direction oldFace = sourceState.getValue(BlockStateProperties.FACING);
                Direction newFace = newState.getValue(BlockStateProperties.FACING);
                if (oldFace != newFace) {
                    invalidReason = "move_fail_surface";
                    showRedOutline(pos, attachFace);
                    return;
                }

                // Check: each gauge position within 24 blocks of target
                for (FactoryPanelPosition gaugePos : gaugeSupport.getLinkedPanels()) {
                    if (!gaugePos.pos().closerThan(pos, 24)) {
                        invalidReason = "move_fail_range";
                        showRedOutline(pos, attachFace);
                        return;
                    }
                }
            }
        }

        // Check: player proximity to source (uses same range as server)
        if (mc.player.distanceToSqr(sourcePos.getX(), sourcePos.getY(), sourcePos.getZ()) > effectiveRange * effectiveRange) {
            invalidReason = "move_fail_range";
            showRedOutline(pos, attachFace);
            return;
        }

        // Check: source-to-target distance (sublevel-aware)
        if (SableHelper.distanceSquared(mc.level, Vec3.atCenterOf(sourcePos), Vec3.atCenterOf(pos)) > effectiveRange * effectiveRange) {
            invalidReason = "move_fail_range";
            showRedOutline(pos, attachFace);
            return;
        }

        // All checks passed
        validTarget = pos;
        validFace = clickedFace;

        Outliner.getInstance()
                .showAABB("target", getFaceHighlight(pos, attachFace))
                .colored(0xeeeeee)
                .disableLineNormals()
                .lineWidth(1 / 16f);
    }

    private static void showRedOutline(BlockPos pos, Direction face) {
        Outliner.getInstance()
                .showAABB("target", getFaceHighlight(pos, face))
                .colored(0xff4444)
                .lineWidth(1 / 16f);
    }

    private static AABB getFaceHighlight(BlockPos pos, Direction face) {
        Vec3 c = Vec3.atCenterOf(pos);
        double minX = c.x - 0.5 + HIGHLIGHT_INSET;
        double minY = c.y - 0.5 + HIGHLIGHT_INSET;
        double minZ = c.z - 0.5 + HIGHLIGHT_INSET;
        double maxX = c.x + 0.5 - HIGHLIGHT_INSET;
        double maxY = c.y + 0.5 - HIGHLIGHT_INSET;
        double maxZ = c.z + 0.5 - HIGHLIGHT_INSET;

        switch (face) {
            case DOWN:  maxY = c.y - 0.5 + HIGHLIGHT_DEPTH; break;
            case UP:    minY = c.y + 0.5 - HIGHLIGHT_DEPTH; break;
            case NORTH: maxZ = c.z - 0.5 + HIGHLIGHT_DEPTH; break;
            case SOUTH: minZ = c.z + 0.5 - HIGHLIGHT_DEPTH; break;
            case WEST:  maxX = c.x - 0.5 + HIGHLIGHT_DEPTH; break;
            case EAST:  minX = c.x + 0.5 - HIGHLIGHT_DEPTH; break;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean onRightClick() {
        if (!active || sourcePos == null) return false;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player.isShiftKeyDown()) {
            validTarget = null;
            validFace = null;
            invalidReason = null;
        }

        if (validTarget != null && validFace != null) {
            PacketDistributor.sendToServer(new RedstoneLinkMovePayload(sourcePos, validTarget, validFace));
            mc.player.displayClientMessage(
                    Component.translatable("gui.createredstonelinkgui.link_relocated"), true);
        } else if (invalidReason != null) {
            mc.player.displayClientMessage(
                    Component.translatable("gui.createredstonelinkgui." + invalidReason), true);
        } else {
            mc.player.displayClientMessage(
                    Component.translatable("gui.createredstonelinkgui.relocation_aborted"), true);
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
        invalidReason = null;
        moveRange = 0;
    }

    public static boolean isActive() {
        return active;
    }
}