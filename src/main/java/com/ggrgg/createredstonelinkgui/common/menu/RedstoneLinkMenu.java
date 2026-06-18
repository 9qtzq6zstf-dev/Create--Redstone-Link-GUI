package com.ggrgg.createredstonelinkgui.common.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

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

public class RedstoneLinkMenu extends AbstractContainerMenu {
    
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "createredstonelinkgui");
    
    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneLinkMenu>> TYPE = MENUS.register("redstone_link_menu", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new RedstoneLinkMenu(windowId, inv, data.readBlockPos()))
    );

    private final BlockPos pos;
    private LinkBehaviour behaviour;
    private boolean isRedstoneLink;
    private boolean receiverMode;

    // Reflection caches
    private static Method cachedSetFrequencyMethod;
    private static Field cachedFirstFreqField;
    private static Field cachedLastFreqField;
    private static Method cachedGetStackMethod;
    private static Method cachedNotifyNetworkMethod;

    static {
        // Eagerly initialize all reflection on class load, ensuring zero runtime overhead
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
            cachedNotifyNetworkMethod = LinkBehaviour.class.getDeclaredMethod("notifySignalChange");
            cachedNotifyNetworkMethod.setAccessible(true);
        } catch (Exception ignored) {}
        // Resolve getStack() on the Frequency class eagerly using the field type
        try {
            Field sampleField = cachedFirstFreqField != null ? cachedFirstFreqField : cachedLastFreqField;
            if (sampleField != null) {
                Class<?> frequencyClass = sampleField.getType();
                cachedGetStackMethod = frequencyClass.getMethod("getStack");
            }
        } catch (Exception ignored) {}
    }


    public BlockPos getPos() { return this.pos; }
    public LinkBehaviour getBehaviour() { return this.behaviour; }
    public boolean isRedstoneLink() { return this.isRedstoneLink; }
    public boolean isReceiverMode() { return this.receiverMode; }

    public RedstoneLinkMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(TYPE.get(), containerId);
        this.pos = pos;
        
        var level = playerInventory.player.level();
        var state = level.getBlockState(pos);
        this.isRedstoneLink = state.getBlock() instanceof RedstoneLinkBlock;
        this.receiverMode = this.isRedstoneLink && state.getValue(RedstoneLinkBlock.RECEIVER);
        
        var be = level.getBlockEntity(pos);
        if (be != null) {
            this.behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
        }

        // Add custom Ghost Recipe Slots
        this.addSlot(new GhostRecipeSlot(0, 98, 34, () -> getFrequencyItem(0), this::setFrequencyItem));
        this.addSlot(new GhostRecipeSlot(1, 134, 34, () -> getFrequencyItem(1), this::setFrequencyItem));

        // Player Main Inventory Layout (Rows 1-3)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, 112 + row * 18));
            }
        }

        // Player Hotbar Layout (Row 4)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 48 + col * 18, 170));
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

        // Access the frequencyFirst / frequencyLast field directly
        // The fields hold Frequency objects (inner class of RedstoneLinkNetworkHandler),
        // which have a public getStack() method that returns the ItemStack.
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
        if (behaviour == null) return;
        
        applyFrequencyChangeDirect(behaviour, index == 0, stack);
    }

    /**
     * Centralized execution pipeline accessible by UI elements and 
     * packet channels to eliminate duplicated reflection code entirely.
     */
    public static void applyFrequencyChangeDirect(LinkBehaviour targetBehaviour, boolean isFirstSlot, ItemStack item) {
        // Reflection already initialized in static block — use cached methods directly
        
        // Preferred: use the public setFrequency(boolean, ItemStack) method
        if (cachedSetFrequencyMethod != null) {
            try {
                cachedSetFrequencyMethod.invoke(targetBehaviour, isFirstSlot, item.copy());
                return;
            } catch (Exception ignored) {}
        }

        // Fallback: directly set the Frequency field
        Field targetField = isFirstSlot ? cachedFirstFreqField : cachedLastFreqField;
        if (targetField != null) {
            try {
                // Use reflection to call Frequency.of(stack) to create the Frequency object
                // The Frequency class is: com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler$Frequency
                // We need to find the static of(ItemStack) method
                Class<?> frequencyClass = targetField.getType();
                if (frequencyClass != null) {
                    Method frequencyOfMethod = frequencyClass.getMethod("of", ItemStack.class);
                    Object frequencyObj = frequencyOfMethod.invoke(null, item.copy());
                    targetField.set(targetBehaviour, frequencyObj);
                }

                if (cachedNotifyNetworkMethod != null) {
                    cachedNotifyNetworkMethod.invoke(targetBehaviour);
                }
            } catch (Exception e) {
                System.err.println("[Redstone Link GUI] Field mutation fallback failure: " + e.getMessage());
            }
        }
    }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
