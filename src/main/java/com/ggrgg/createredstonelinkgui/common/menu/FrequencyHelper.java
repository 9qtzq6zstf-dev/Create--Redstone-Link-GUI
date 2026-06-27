package com.ggrgg.createredstonelinkgui.common.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.simibubi.create.content.redstone.link.LinkBehaviour;

import net.minecraft.world.item.ItemStack;

/**
 * Unified reflection helper for both Create's LinkBehaviour and Create Utilities'
 * VoidLinkBehaviour. Eliminates duplicated reflection code between RedstoneLinkMenu
 * and VoidLinkMenu (and network handlers).
 * 
 * <p>LinkBehaviour reflection is initialized eagerly on class load.
 * VoidLinkBehaviour reflection is initialized lazily on first use (since the
 * Create Utilities mod is optional).
 */
public class FrequencyHelper {

    // ==================== LinkBehaviour (Create) Reflection ====================

    private static boolean linkInit = false;
    private static Method LINK_setFrequencyMethod;
    private static Field LINK_firstFreqField;
    private static Field LINK_lastFreqField;
    private static Method LINK_notifyNetworkMethod;

    // ==================== VoidLinkBehaviour (Create Utilities) Reflection ====================

    private static boolean voidInit = false;
    private static Method VOID_setFrequencyMethod;
    private static Field VOID_firstFreqField;
    private static Field VOID_lastFreqField;

    // ==================== Shared: Frequency inner class ====================

    private static Method cachedGetStackMethod;
    private static Method cachedFrequencyOfMethod;

    // ==================== LinkBehaviour Init (Eager) ====================

    private static void initLinkReflection() {
        if (linkInit) return;
        linkInit = true;
        try {
            LINK_setFrequencyMethod = LinkBehaviour.class.getMethod("setFrequency", boolean.class, ItemStack.class);
        } catch (Exception ignored) {}
        try {
            LINK_firstFreqField = LinkBehaviour.class.getDeclaredField("frequencyFirst");
            LINK_firstFreqField.setAccessible(true);
        } catch (Exception ignored) {}
        try {
            LINK_lastFreqField = LinkBehaviour.class.getDeclaredField("frequencyLast");
            LINK_lastFreqField.setAccessible(true);
        } catch (Exception ignored) {}
        try {
            LINK_notifyNetworkMethod = LinkBehaviour.class.getDeclaredMethod("notifySignalChange");
            LINK_notifyNetworkMethod.setAccessible(true);
        } catch (Exception ignored) {}
        // Resolve getStack() and of() on the Frequency class from the field type
        initFrequencyClassMethods();
    }

    // ==================== VoidLinkBehaviour Init (Lazy) ====================

    private static void initVoidReflection() {
        if (voidInit) return;
        voidInit = true;
        try {
            Class<?> vlbClass = Class.forName("me.duquee.createutilities.blocks.voidtypes.VoidLinkBehaviour");
            try { VOID_setFrequencyMethod = vlbClass.getMethod("setFrequency", boolean.class, ItemStack.class); } catch (Throwable ignored) {}
            try { VOID_firstFreqField = vlbClass.getDeclaredField("frequencyFirst"); VOID_firstFreqField.setAccessible(true); } catch (Throwable ignored) {}
            try { VOID_lastFreqField = vlbClass.getDeclaredField("frequencyLast"); VOID_lastFreqField.setAccessible(true); } catch (Throwable ignored) {}
            // Resolve getStack() and of() from the field type
            initFrequencyClassMethods();
        } catch (Throwable ignored) {}
    }

    /**
     * Try to resolve the Frequency inner class methods (getStack, of) from whichever
     * field we have available. Safe to call multiple times — only sets if null.
     */
    private static void initFrequencyClassMethods() {
        if (cachedGetStackMethod != null) return; // Already resolved
        Field sampleField = LINK_firstFreqField != null ? LINK_firstFreqField
            : LINK_lastFreqField != null ? LINK_lastFreqField
            : VOID_firstFreqField != null ? VOID_firstFreqField
            : VOID_lastFreqField;
        if (sampleField == null) return;
        try {
            Class<?> frequencyClass = sampleField.getType();
            cachedGetStackMethod = frequencyClass.getMethod("getStack");
            cachedFrequencyOfMethod = frequencyClass.getMethod("of", ItemStack.class);
        } catch (Exception ignored) {}
    }

    // ==================== Public API ====================

    /**
     * Get the frequency ItemStack from the given behaviour.
     * Works with both LinkBehaviour and VoidLinkBehaviour.
     */
    public static ItemStack getFrequencyItem(Object behaviour, int index) {
        if (behaviour == null) return ItemStack.EMPTY;

        // Determine which reflection set to use
        boolean isLink = behaviour instanceof LinkBehaviour;

        if (isLink) {
            initLinkReflection();
        } else {
            initVoidReflection();
        }

        Field targetField = getFirstField(isLink);
        if (index != 0) targetField = getLastField(isLink);

        if (targetField != null) {
            try {
                Object frequency = targetField.get(behaviour);
                if (frequency != null && cachedGetStackMethod != null) {
                    ItemStack stack = (ItemStack) cachedGetStackMethod.invoke(frequency);
                    if (stack != null && !stack.isEmpty()) return stack;
                }
            } catch (Exception ignored) {}
        }
        return ItemStack.EMPTY;
    }

    /**
     * Set the frequency on the given behaviour and notify the network.
     * Works with both LinkBehaviour and VoidLinkBehaviour.
     */
    public static void setFrequencyItem(Object behaviour, int index, ItemStack stack) {
        if (behaviour == null) return;
        applyFrequencyChangeDirect(behaviour, index == 0, stack);
    }

    /**
     * Centralized execution pipeline for applying frequency changes.
     * Works with both LinkBehaviour and VoidLinkBehaviour.
     */
    public static void applyFrequencyChangeDirect(Object targetBehaviour, boolean isFirstSlot, ItemStack item) {
        if (targetBehaviour == null) return;

        boolean isLink = targetBehaviour instanceof LinkBehaviour;

        if (isLink) {
            initLinkReflection();
        } else {
            initVoidReflection();
        }

        Method setFreqMethod = isLink ? LINK_setFrequencyMethod : VOID_setFrequencyMethod;

        // Preferred: use the public setFrequency(boolean, ItemStack) method
        if (setFreqMethod != null) {
            try {
                setFreqMethod.invoke(targetBehaviour, isFirstSlot, item.copy());
                notifySignalIfLink(targetBehaviour);
                return;
            } catch (Exception ignored) {}
        }

        // Fallback: directly set the Frequency field
        Field targetField = isFirstSlot ? getFirstField(isLink) : getLastField(isLink);
        if (targetField != null) {
            try {
                if (cachedFrequencyOfMethod != null) {
                    Object frequencyObj = cachedFrequencyOfMethod.invoke(null, item.copy());
                    targetField.set(targetBehaviour, frequencyObj);
                }
                notifySignalIfLink(targetBehaviour);
            } catch (Exception e) {
                System.err.println("[FrequencyHelper] Field mutation fallback failure: " + e.getMessage());
            }
        }
    }

    // ==================== Private helpers ====================

    private static void notifySignalIfLink(Object behaviour) {
        if (behaviour instanceof LinkBehaviour && LINK_notifyNetworkMethod != null) {
            try {
                LINK_notifyNetworkMethod.invoke(behaviour);
            } catch (Exception ignored) {}
        }
    }

    private static Field getFirstField(boolean isLink) {
        return isLink ? LINK_firstFreqField : VOID_firstFreqField;
    }

    private static Field getLastField(boolean isLink) {
        return isLink ? LINK_lastFreqField : VOID_lastFreqField;
    }
}