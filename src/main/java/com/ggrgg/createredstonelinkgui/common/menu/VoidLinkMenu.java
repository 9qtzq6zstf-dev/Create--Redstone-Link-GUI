package com.ggrgg.createredstonelinkgui.common.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VoidLinkMenu extends AbstractContainerMenu {
    
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "createredstonelinkgui");
    
    public static final DeferredHolder<MenuType<?>, MenuType<VoidLinkMenu>> TYPE = MENUS.register("void_link_menu", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new VoidLinkMenu(windowId, inv, data.readBlockPos()))
    );

    private final BlockPos pos;
    private Object behaviour;

    private static boolean reflectionInit = false;
    private static Method cachedSetFrequencyMethod;
    private static Field cachedFirstFreqField;
    private static Field cachedLastFreqField;
    private static Method cachedGetStackMethod;

    private static void initReflection() {
        if (reflectionInit) return;
        reflectionInit = true;
        try {
            Class<?> vlbClass = Class.forName("me.duquee.createutilities.blocks.voidtypes.VoidLinkBehaviour");
            // Each lookup is individually guarded — VoidLinkBehaviour doesn't have notifySignalChange
            try { cachedSetFrequencyMethod = vlbClass.getMethod("setFrequency", boolean.class, ItemStack.class); } catch (Throwable ignored) {}
            try { cachedFirstFreqField = vlbClass.getDeclaredField("frequencyFirst"); cachedFirstFreqField.setAccessible(true); } catch (Throwable ignored) {}
            try { cachedLastFreqField = vlbClass.getDeclaredField("frequencyLast"); cachedLastFreqField.setAccessible(true); } catch (Throwable ignored) {}
            // Resolve getStack() from the Frequency class (same across both mods)
            try {
                Field sample = cachedFirstFreqField != null ? cachedFirstFreqField : cachedLastFreqField;
                if (sample != null) {
                    cachedGetStackMethod = sample.getType().getMethod("getStack");
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public BlockPos getPos() { return this.pos; }
    public Object getBehaviour() { return this.behaviour; }

    public VoidLinkMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(TYPE.get(), containerId);
        this.pos = pos;
        
        var level = playerInventory.player.level();
        
        this.behaviour = VoidLinkHelper.getBehaviour(level, pos);

        this.addSlot(new GhostRecipeSlot(0, 101, 34, () -> getFrequencyItem(0), this::setFrequencyItem));
        this.addSlot(new GhostRecipeSlot(1, 137, 34, () -> getFrequencyItem(1), this::setFrequencyItem));

        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, 112 + row * 18));

        for (int col = 0; col < 9; ++col)
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 48 + col * 18, 170));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 2) {
            var slot = this.getSlot(slotId);
            ItemStack targetStack = ItemStack.EMPTY;
            if (button == 1 || clickType == ClickType.THROW) {
                slot.set(ItemStack.EMPTY);
            } else {
                targetStack = getCarried().copy();
                slot.set(targetStack);
            }
            if (player.level().isClientSide())
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload(this.pos, targetStack, slotId));
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }
    
    private ItemStack getFrequencyItem(int index) {
        initReflection();
        if (behaviour == null) return ItemStack.EMPTY;
        Field targetField = (index == 0) ? cachedFirstFreqField : cachedLastFreqField;
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

    private void setFrequencyItem(int index, ItemStack stack) {
        initReflection();
        if (behaviour == null) return;
        applyFrequencyChangeDirect(behaviour, index == 0, stack);
    }

    public static void applyFrequencyChangeDirect(Object targetBehaviour, boolean isFirstSlot, ItemStack item) {
        initReflection();
        if (cachedSetFrequencyMethod != null) {
            try {
                cachedSetFrequencyMethod.invoke(targetBehaviour, isFirstSlot, item.copy());
                return;
            } catch (Exception ignored) {}
        }
        Field targetField = isFirstSlot ? cachedFirstFreqField : cachedLastFreqField;
        if (targetField != null) {
            try {
                Class<?> frequencyClass = targetField.getType();
                if (frequencyClass != null) {
                    Method frequencyOfMethod = frequencyClass.getMethod("of", ItemStack.class);
                    targetField.set(targetBehaviour, frequencyOfMethod.invoke(null, item.copy()));
                }
            } catch (Exception e) {
                System.err.println("[Redstone Link GUI] Void link fallback failure: " + e.getMessage());
            }
        }
    }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}