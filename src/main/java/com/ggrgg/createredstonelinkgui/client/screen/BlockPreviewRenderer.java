package com.ggrgg.createredstonelinkgui.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockPreviewRenderer {
    private static final String CREATE_CONNECTED = "create_connected";
    private static final String LINKED_ANALOG_LEVER = "linked_analog_lever";
    private static final String LINKED_THROTTLE_LEVER = "linked_throttle_lever";
    private static final PartialModel THROTTLE_LEVER_HANDLE =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("simulated", "block/throttle_lever/handle"));
    private static final PartialModel THROTTLE_LEVER_BUTTON =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("simulated", "block/throttle_lever/button"));
    private static final PartialModel THROTTLE_LEVER_DIODE =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("simulated", "block/throttle_lever/diode"));
    private static final int LINKED_PREVIEW_X_OFFSET = 27;
    private static final int LINKED_PREVIEW_Y_OFFSET = 62;
    private static final int LINKED_PREVIEW_Y_ROTATION = 135;

    public static void render(GuiGraphics graphics, BlockState blockState, BlockEntity blockEntity, int x, int y) {
        if (isCreateConnectedLinkedControl(blockState)) {
            BlockState previewState = getLinkedPreviewState(blockState);
            int previewX = x + LINKED_PREVIEW_X_OFFSET;
            int previewY = y + LINKED_PREVIEW_Y_OFFSET;

            GuiGameElement.of(previewState)
                    .rotateBlock(25, LINKED_PREVIEW_Y_ROTATION, 0)
                    .scale(42)
                    .atLocal(-0.5, -0.35, 0)
                    .render(graphics, previewX, previewY);

            if (isLinkedAnalogLever(blockState)) {
                renderAnalogLeverParts(graphics, previewState, blockEntity, previewX, previewY);
            } else if (isLinkedThrottleLever(blockState)) {
                renderThrottleLeverParts(graphics, previewState, blockEntity, previewX, previewY);
            }
            return;
        }

        ItemStack blockStack = new ItemStack(blockState.getBlock().asItem());
        if (!blockStack.isEmpty()) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, 0, 10);
            GuiGameElement.of(blockStack)
                    .scale(4)
                    .at(0, 0, -200)
                    .render(graphics, x, y);
            pose.popPose();
        }
    }

    private static boolean isCreateConnectedLinkedControl(BlockState blockState) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (!CREATE_CONNECTED.equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();
        return path.equals("linked_lever")
                || path.equals(LINKED_ANALOG_LEVER)
                || path.equals(LINKED_THROTTLE_LEVER)
                || path.startsWith("linked_") && path.endsWith("_button");
    }

    private static boolean isLinkedAnalogLever(BlockState blockState) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        return CREATE_CONNECTED.equals(id.getNamespace()) && LINKED_ANALOG_LEVER.equals(id.getPath());
    }

    private static boolean isLinkedThrottleLever(BlockState blockState) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        return CREATE_CONNECTED.equals(id.getNamespace()) && LINKED_THROTTLE_LEVER.equals(id.getPath());
    }

    private static void renderAnalogLeverParts(GuiGraphics graphics, BlockState previewState, BlockEntity blockEntity,
            int x, int y) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        prepareLinkedPreviewMatrix(pose, x, y);

        MultiBufferSource.BufferSource buffer = graphics.bufferSource();
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        float state = getAnalogLeverState(blockEntity);

        SuperByteBuffer handle = transformAnalogLeverPart(CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_HANDLE, previewState),
                previewState);
        float angle = (float) ((state / 15) * 90 / 180 * Math.PI);
        handle.translate(1 / 2f, 1 / 16f, 1 / 2f)
                .rotate(angle, Direction.EAST)
                .translate(-1 / 2f, -1 / 16f, -1 / 2f)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(pose, vb);

        int color = Color.mixColors(0x2C0300, 0xCD0000, state / 15f);
        transformAnalogLeverPart(CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_INDICATOR, previewState), previewState)
                .light(LightTexture.FULL_BRIGHT)
                .color(color)
                .renderInto(pose, vb);

        buffer.endBatch();
        pose.popPose();
    }

    private static void renderThrottleLeverParts(GuiGraphics graphics, BlockState previewState, BlockEntity blockEntity,
            int x, int y) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        prepareLinkedPreviewMatrix(pose, x, y);

        MultiBufferSource.BufferSource buffer = graphics.bufferSource();
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        float state = getThrottleLeverState(blockEntity);
        float signal = Math.max(0, state / 15f);
        float angle = (float) (((state / 15f) * 80 - 40) / 180f * Math.PI);

        SuperByteBuffer handle = transformThrottleLeverPart(CachedBuffers.partial(THROTTLE_LEVER_HANDLE, previewState),
                previewState);
        transformThrottleHandle(handle, angle, previewState)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(pose, vb);

        SuperByteBuffer button = transformThrottleLeverPart(CachedBuffers.partial(THROTTLE_LEVER_BUTTON, previewState),
                previewState);
        transformThrottleHandle(button, angle, previewState)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(pose, vb);

        transformThrottleLeverPart(CachedBuffers.partial(THROTTLE_LEVER_DIODE, previewState), previewState)
                .light(LightTexture.FULL_BRIGHT)
                .color(redstoneColor(signal))
                .renderInto(pose, vb);

        buffer.endBatch();
        pose.popPose();
    }

    private static void prepareLinkedPreviewMatrix(PoseStack pose, int x, int y) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Lighting.setupFor3DItems();

        pose.translate(x, y, 0);
        pose.scale(42, 42, 42);
        pose.translate(-0.5, -0.35, 0);
        UIRenderHelper.flipForGuiRender(pose);
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(25));
        pose.mulPose(Axis.YP.rotationDegrees(LINKED_PREVIEW_Y_ROTATION));
        pose.translate(-0.5, -0.5, -0.5);
    }

    private static float getAnalogLeverState(BlockEntity blockEntity) {
        if (blockEntity instanceof AnalogLeverBlockEntity analogLever) {
            return analogLever.getState();
        }
        return 0;
    }

    private static float getThrottleLeverState(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return 0;
        }

        try {
            Object value = blockEntity.getClass().getMethod("getState").invoke(blockEntity);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0;
    }

    private static SuperByteBuffer transformAnalogLeverPart(SuperByteBuffer buffer, BlockState leverState) {
        AttachFace face = leverState.getValue(BlockStateProperties.ATTACH_FACE);
        float rX = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
        float rY = AngleHelper.horizontalAngle(leverState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        buffer.rotateCentered((float) (rY / 180 * Math.PI), Direction.UP);
        buffer.rotateCentered((float) (rX / 180 * Math.PI), Direction.EAST);
        return buffer;
    }

    private static SuperByteBuffer transformThrottleLeverPart(SuperByteBuffer buffer, BlockState leverState) {
        AttachFace face = leverState.getValue(BlockStateProperties.ATTACH_FACE);
        float rX = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
        float rY = AngleHelper.horizontalAngle(leverState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        buffer.rotateCentered((float) (rY / 180 * Math.PI), Direction.UP);
        buffer.rotateCentered((float) (rX / 180 * Math.PI), Direction.EAST);
        buffer.rotateCentered(face == AttachFace.CEILING ? (float) Math.PI : 0, Direction.UP);
        return buffer;
    }

    private static SuperByteBuffer transformThrottleHandle(SuperByteBuffer buffer, float angle, BlockState leverState) {
        AttachFace face = leverState.getValue(BlockStateProperties.ATTACH_FACE);
        float adjustedAngle = face == AttachFace.WALL ? -angle : angle;
        buffer.translate(0.5f, 0.1875f, 0.5f)
                .rotateX(adjustedAngle)
                .translateBack(0.5f, 0.1875f, 0.5f)
                .rotateCentered(face == AttachFace.WALL ? (float) Math.PI : 0, Direction.UP);
        return buffer;
    }

    private static int redstoneColor(float signal) {
        return Color.mixColors(0x2C0300, 0xCD0000, signal);
    }

    private static BlockState getLinkedPreviewState(BlockState blockState) {
        BlockState previewState = blockState.getBlock().defaultBlockState();
        previewState = setIfPresent(previewState, BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        previewState = setIfPresent(previewState, BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        previewState = setIfPresent(previewState, BlockStateProperties.POWERED, false);
        return setBooleanPropertyIfPresent(previewState, "locked", false);
    }

    private static <T extends Comparable<T>> BlockState setIfPresent(BlockState state, Property<T> property, T value) {
        if (state.hasProperty(property)) {
            return state.setValue(property, value);
        }
        return state;
    }

    private static BlockState setBooleanPropertyIfPresent(BlockState state, String name, boolean value) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name) && property.getPossibleValues().contains(value)) {
                return setRaw(state, property, value);
            }
        }
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState setRaw(BlockState state, Property property, Comparable value) {
        return state.setValue(property, value);
    }
}
