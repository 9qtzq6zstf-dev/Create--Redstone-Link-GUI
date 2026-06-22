package com.ggrgg.createredstonelinkgui.common;

import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility for Sable sublevel compatibility.
 * Uses SableCompanion via reflection, following the official sable-companion API:
 * https://github.com/ryanhcode/sable-companion
 */
public class SableHelper {

    private static boolean checked = false;
    private static Method distanceSqMethod;
    private static Object instance;

    private static void check() {
        if (checked) return;
        checked = true;
        try {
            Class<?> companionClass = Class.forName("dev.ryanhcode.sable.companion.SableCompanion");
            Field instField = companionClass.getField("INSTANCE");
            instance = instField.get(null);
            // Uses Position interface (implemented by Vec3) as per the official guide
            distanceSqMethod = companionClass.getMethod("distanceSquaredWithSubLevels", Level.class, Position.class, Position.class);
        } catch (Throwable ignored) {}
    }

    /**
     * Computes distance squared between two positions, accounting for sublevel offsets.
     * Follows the official SableCompanion API pattern.
     */
    public static double distanceSquared(Level level, Vec3 a, Vec3 b) {
        if (checked && instance == null) return a.distanceToSqr(b);
        check();
        if (instance == null) return a.distanceToSqr(b);
        try {
            return (double) distanceSqMethod.invoke(instance, level, a, b);
        } catch (Throwable ignored) {}
        return a.distanceToSqr(b);
    }
}