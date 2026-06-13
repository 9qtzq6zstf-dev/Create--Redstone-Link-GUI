package com.ggrgg.createredstonelinkgui.common.network;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RedstoneLinkMovePayload(BlockPos sourcePos, BlockPos targetPos, Direction clickedFace) implements CustomPacketPayload {

    public static final Type<RedstoneLinkMovePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "link_move"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RedstoneLinkMovePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RedstoneLinkMovePayload::sourcePos,
            BlockPos.STREAM_CODEC, RedstoneLinkMovePayload::targetPos,
            ByteBufCodecs.VAR_INT, p -> p.clickedFace().get3DDataValue(),
            (pos1, pos2, face) -> new RedstoneLinkMovePayload(pos1, pos2, Direction.from3DDataValue(face))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(RedstoneLinkMovePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            BlockPos sourcePos = payload.sourcePos();
            BlockPos targetPos = payload.targetPos();
            Direction clickedFace = payload.clickedFace();

            // Verification: within interaction range
            if (player.distanceToSqr(sourcePos.getX(), sourcePos.getY(), sourcePos.getZ()) > 64.0) return;
            if (!targetPos.closerThan(sourcePos, 24)) return;
            if (!level.isLoaded(targetPos)) return;

            BlockEntity sourceBE = level.getBlockEntity(sourcePos);
            if (sourceBE == null) return;

            // Check that source has a LinkBehaviour (universal discriminant)
            LinkBehaviour sourceLink = BlockEntityBehaviour.get(sourceBE, LinkBehaviour.TYPE);
            if (sourceLink == null) return;

            BlockState sourceState = sourceBE.getBlockState();
            BlockState targetState = level.getBlockState(targetPos);
            boolean inPlace = sourcePos.equals(targetPos);

            // If moving to a new position, it must be air or replaceable
            // If moving in place, allow it (reorienting without moving)
            if (!inPlace && !targetState.isAir() && !targetState.canBeReplaced()) return;

            // Dynamically determine the new block state using the block's own placement logic.
            // This works universally — each block's getStateForPlacement() handles its
            // specific orientation constraints (FACING, FACE, etc.).
            Block block = sourceState.getBlock();
            BlockPlaceContext placeContext = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
                    ItemStack.EMPTY, new BlockHitResult(Vec3.atCenterOf(targetPos), clickedFace, targetPos, false));
            BlockState newState = block.getStateForPlacement(placeContext);
            if (newState == null) return;

            // Preserve blockstate properties that aren't orientation-related
            // (e.g. POWERED visual state, RECEIVER mode)
            newState = copyNonOrientationProperties(newState, sourceState);

            // Validate survivability with the new orientation
            if (!newState.canSurvive(level, targetPos)) return;

            // Capture full BE data (saves all NBT including LinkBehaviour frequencies,
            // block-specific fields like MinRange/MaxRange, Transmitter, etc.)
            HolderLookup.Provider registries = level.registryAccess();
            CompoundTag beTag = sourceBE.saveWithoutMetadata(registries);

            // Place block at target with dynamically determined state
            level.setBlock(targetPos, newState, Block.UPDATE_ALL);

            // Restore NBT on new BE.
            // After loadWithComponents(), the LinkBehaviour's read() checks LastKnownPosition
            // against the new position → newPosition = true.
            // On the next tick, initialize() → addToNetwork() → network auto re-registers.
            BlockEntity newBE = level.getBlockEntity(targetPos);
            if (newBE != null) {
                newBE.loadWithComponents(beTag, registries);
                newBE.setChanged();
            }

            // Notify neighbors at new position
            level.sendBlockUpdated(targetPos, newState, newState, Block.UPDATE_ALL);

            // If moving in place, we're done — the block state has been updated with new orientation
            // Otherwise, destroy source block silently (no drops)
            // Use UPDATE_MOVE_BY_PISTON to prevent blocks like Create: Connected's linked transmitter
            // from dropping their transmitter item in onRemove() when the block is moved
            if (!inPlace) {
                level.setBlock(sourcePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_MOVE_BY_PISTON);
                level.sendBlockUpdated(sourcePos, sourceState, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
    }

    private static BlockState copyNonOrientationProperties(BlockState target, BlockState source) {
        BlockState result = target;
        for (Property<?> property : source.getProperties()) {
            String name = property.getName();
            if (name.equals("facing") || name.equals("face") || name.equals("attachment"))
                continue;
            if (result.hasProperty(property)) {
                result = copyPropertyUntyped(result, source, property);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyPropertyUntyped(BlockState target, BlockState source, Property<?> property) {
        Property<T> typed = (Property<T>) property;
        return target.setValue(typed, source.getValue(typed));
    }
}