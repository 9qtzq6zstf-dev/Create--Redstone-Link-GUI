package com.ggrgg.createredstonelinkgui.common.menu;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RedstoneLinkMenu extends AbstractContainerMenu {
    
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "createredstonelinkgui");
    
    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneLinkMenu>> TYPE = MENUS.register("redstone_link_menu", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new RedstoneLinkMenu(windowId, inv, data.readBlockPos()))
    );

    private final BlockPos pos;
    private LinkBehaviour behaviour;
    private RedstoneLinkBlockEntity blockEntity;

    // High Performance Optimization: Cache all reflection points statically during class loading
    private static Method cachedGetFrequencyMethod;
    private static Method cachedSetFrequencyMethod;
    private static Field cachedFirstFreqField;
    private static Field cachedLastFreqField;
    private static Method cachedGetFilterMethod;
    private static Method cachedSetFilterMethod;
    private static Method cachedNotifyNetworkMethod;
    private static Field cachedFrequenciesArrayField;
    private static boolean reflectionInitialized = false;

    public static synchronized void initReflection() {
        if (reflectionInitialized) return;
        try {
            cachedGetFrequencyMethod = LinkBehaviour.class.getMethod("getFrequency", boolean.class);
        } catch (Exception ignored) {}
        try {
            cachedSetFrequencyMethod = LinkBehaviour.class.getMethod("setFrequency", boolean.class, ItemStack.class);
        } catch (Exception ignored) {}
        try {
            cachedFirstFreqField = LinkBehaviour.class.getDeclaredField("frequencyFirst");
            cachedFirstFreqField.setAccessible(true);
        } catch (Exception ignored) {}
        try {
            cachedLastFreqField = LinkBehaviour.class.getDeclaredField("frequencyLast");
            cachedLastFreqField.setAccessible(true);
        } catch (Exception ignored) {}
        try {
            cachedNotifyNetworkMethod = LinkBehaviour.class.getDeclaredMethod("notifyNetwork");
            cachedNotifyNetworkMethod.setAccessible(true);
        } catch (Exception ignored) {}
        try {
            cachedFrequenciesArrayField = LinkBehaviour.class.getDeclaredField("frequencies");
            cachedFrequenciesArrayField.setAccessible(true);
        } catch (Exception ignored) {}
        reflectionInitialized = true;
    }

    public BlockPos getPos() { return this.pos; }
    public LinkBehaviour getBehaviour() { return this.behaviour; }

    public RedstoneLinkMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(TYPE.get(), containerId);
        this.pos = pos;
        initReflection();
        
        var level = playerInventory.player.level();
        if (level.getBlockEntity(pos) instanceof RedstoneLinkBlockEntity le) {
            this.blockEntity = le;
            this.behaviour = BlockEntityBehaviour.get(le, LinkBehaviour.TYPE);
        }

        // Add custom Ghost Recipe Slots (Shifted by 1px from sprite coordinates to perfectly center item graphics)
        this.addSlot(new GhostRecipeSlot(0, 54, 26, () -> getFrequencyItem(0), this::setFrequencyItem));
        this.addSlot(new GhostRecipeSlot(1, 108, 26, () -> getFrequencyItem(1), this::setFrequencyItem));

        // Player Main Inventory Layout (Rows 1-3)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar Layout (Row 4)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 2) {
            var slot = this.getSlot(slotId);
            ItemStack targetStack = ItemStack.EMPTY;
            
            if (button == 1 || clickType == ClickType.THROW) { 
                slot.set(ItemStack.EMPTY);
            } else {
                ItemStack carried = getCarried();
                targetStack = carried.copy();
                slot.set(targetStack);
            }

            if (player.level().isClientSide()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload(this.pos, targetStack, slotId)
                );
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }
    
    private ItemStack getFrequencyItem(int index) {
        if (behaviour == null) return ItemStack.EMPTY;
        
        // Dynamic Strategy 1: Cached native getter
        if (cachedGetFrequencyMethod != null) {
            try {
                ItemStack stack = (ItemStack) cachedGetFrequencyMethod.invoke(behaviour, index == 0);
                if (stack != null && !stack.isEmpty()) return stack;
            } catch (Exception ignored) {}
        }

        // Dynamic Strategy 2: Internal Filtering Behaviour components
        Field targetField = (index == 0) ? cachedFirstFreqField : cachedLastFreqField;
        if (targetField != null) {
            try {
                Object filteringBehaviour = targetField.get(behaviour);
                if (filteringBehaviour != null) {
                    if (cachedGetFilterMethod == null) {
                        cachedGetFilterMethod = filteringBehaviour.getClass().getMethod("getFilter");
                    }
                    ItemStack stack = (ItemStack) cachedGetFilterMethod.invoke(filteringBehaviour);
                    if (stack != null && !stack.isEmpty()) return stack;
                }
            } catch (Exception ignored) {}
        }

        // Dynamic Strategy 3: Structural wireless dynamic fallback matrix
        if (cachedFrequenciesArrayField != null) {
            try {
                Object rawArray = cachedFrequenciesArrayField.get(behaviour);
                if (rawArray instanceof Object[] components && index < components.length) {
                    Object singleComponent = components[index];
                    if (singleComponent != null) {
                        try {
                            Method getStack = singleComponent.getClass().getMethod("getStack");
                            return (ItemStack) getStack.invoke(singleComponent);
                        } catch (NoSuchMethodException e) {
                            Field stackField = singleComponent.getClass().getDeclaredField("stack");
                            stackField.setAccessible(true);
                            return (ItemStack) stackField.get(singleComponent);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return ItemStack.EMPTY;
    }

    private void setFrequencyItem(int index, ItemStack stack) {
        if (behaviour == null) return;
        
        applyFrequencyChangeDirect(behaviour, index == 0, stack);
        
        if (blockEntity != null) {
            blockEntity.setChanged();
            var level = blockEntity.getLevel();
            if (level != null) {
                level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            }
        }
    }

    /**
     * Centralized execution pipeline accessible by UI elements and 
     * packet channels to eliminate duplicated reflection code entirely.
     */
    public static void applyFrequencyChangeDirect(LinkBehaviour targetBehaviour, boolean isFirstSlot, ItemStack item) {
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
                Object filteringBehaviour = targetField.get(targetBehaviour);
                if (filteringBehaviour != null) {
                    if (cachedSetFilterMethod == null) {
                        cachedSetFilterMethod = filteringBehaviour.getClass().getMethod("setFilter", ItemStack.class);
                    }
                    cachedSetFilterMethod.invoke(filteringBehaviour, item.copy());
                }
                if (cachedNotifyNetworkMethod != null) {
                    cachedNotifyNetworkMethod.invoke(targetBehaviour);
                }
            } catch (Exception e) {
                System.err.println("[Redstone Link GUI] Static mutation map failure: " + e.getMessage());
            }
        }
    }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
