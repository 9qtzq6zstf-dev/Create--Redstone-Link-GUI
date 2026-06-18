package com.ggrgg.createredstonelinkgui.common;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

public class VoidLinkHelper {

    private static boolean checked = false;
    private static Object voidLinkType;
    private static Method blockEntityBehaviourGet;
    private static Method testHitMethod;
    private static Method getOwnerMethod;
    private static Method setOwnerMethod;
    private static Method canInteractMethod;

    private static void check() {
        if (checked) return;
        checked = true;
        try {
            Class<?> vlbClass = Class.forName("me.duquee.createutilities.blocks.voidtypes.VoidLinkBehaviour");
            voidLinkType = vlbClass.getField("TYPE").get(null);
            testHitMethod = vlbClass.getMethod("testHit", int.class, Vec3.class);
            getOwnerMethod = vlbClass.getMethod("getOwner");
            setOwnerMethod = vlbClass.getMethod("setOwner", GameProfile.class);
            canInteractMethod = vlbClass.getMethod("canInteract", Player.class);
            blockEntityBehaviourGet = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.class
                .getMethod("get", net.minecraft.world.level.BlockGetter.class, BlockPos.class,
                    com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType.class);
        } catch (Throwable ignored) {}
    }

    public static Object getBehaviour(Level level, BlockPos pos) {
        check();
        if (voidLinkType == null || blockEntityBehaviourGet == null) return null;
        try {
            return blockEntityBehaviourGet.invoke(null, level, pos, voidLinkType);
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isHitOnFrequencySlot(Object behaviour, Vec3 hitLocation) {
        if (behaviour == null || testHitMethod == null) return false;
        try {
            return (boolean) testHitMethod.invoke(behaviour, 0, hitLocation)
                || (boolean) testHitMethod.invoke(behaviour, 1, hitLocation);
        } catch (Throwable ignored) {}
        return false;
    }

    public static GameProfile getOwner(Object behaviour) {
        if (behaviour == null || getOwnerMethod == null) return null;
        try {
            return (GameProfile) getOwnerMethod.invoke(behaviour);
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean canInteract(Object behaviour, Player player) {
        if (behaviour == null || canInteractMethod == null) return true;
        try {
            return (boolean) canInteractMethod.invoke(behaviour, player);
        } catch (Throwable ignored) {}
        return true;
    }

    public static void setOwner(Object behaviour, GameProfile owner) {
        if (behaviour == null || setOwnerMethod == null) return;
        try {
            setOwnerMethod.invoke(behaviour, owner);
        } catch (Throwable ignored) {}
    }
}