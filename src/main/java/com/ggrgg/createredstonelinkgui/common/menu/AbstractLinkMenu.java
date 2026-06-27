package com.ggrgg.createredstonelinkgui.common.menu;

import com.ggrgg.createredstonelinkgui.common.network.PresetSlotUpdatePayload;
import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Abstract base class for both RedstoneLinkMenu and VoidLinkMenu.
 * Contains all shared layout constants, preset slot logic, frequency slot handling,
 * and player inventory construction.
 */
public abstract class AbstractLinkMenu extends AbstractContainerMenu {

    // Preset slot IDs (2 + 4 rows × 2 columns = slots 2-9)
    public static final int PRESET_SLOT_START = 2;
    public static final int PRESET_SLOTS_PER_ROW = 2;
    public static final int PRESET_ROWS = FrequencyPresetData.PRESET_COUNT;

    // ⚠ MUST MATCH FrequencyPresetPanel layout constants ⚠
    // If you change FrequencyPresetPanel.SLOT_X/PANEL_OFFSET/HEADER_H/SLOT_Y_OFFSET/ROW_H/SLOT_SPACING_X,
    // update these values to match. They cannot reference the panel class directly
    // because this is a common class (server-safe), while FrequencyPresetPanel is client-only.
    //
    // Derivation from panel constants:
    //   X_START = PANEL_OFFSET(-71) + SLOT_X(6) = -65
    //   Y_START = 8 + HEADER_H(36) + SLOT_Y_OFFSET(8) = 52
    //   SPACING_X = SLOT_SPACING_X(26)
    //   SPACING_Y = ROW_H(31)
    public static final int PRESET_SLOT_X_START = -65;
    public static final int PRESET_SLOT_Y_START = 52;
    public static final int PRESET_SLOT_SPACING_X = 26;
    public static final int PRESET_SLOT_SPACING_Y = 31;

    protected final BlockPos pos;
    protected final Player player;

    protected AbstractLinkMenu(int containerId, Inventory playerInventory, BlockPos pos, MenuType<?> menuType) {
        super(menuType, containerId);
        this.pos = pos;
        this.player = playerInventory.player;

        // Preset slots added by helper — subclasses add frequency slots themselves
        addPresetSlots(playerInventory);

        // Player Main Inventory Layout (Rows 1-3)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, 112 + row * 18));
            }
        }

        // Player Hotbar Layout (Row 4)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 48 + col * 18, 170));
        }
    }

    // ==================== Subclass hooks ====================

    /**
     * @return the behaviour object (LinkBehaviour or VoidLinkBehaviour), or null
     */
    protected abstract Object getBehaviour();

    /**
     * @return true if the given slotId is a frequency slot that this base class
     *         should handle with {@link #handleFrequencySlotClick}
     */
    protected boolean isFrequencySlot(int slotId) {
        return slotId >= 0 && slotId < 2;
    }

    // ==================== Preset slots ====================

    private void addPresetSlots(Inventory playerInventory) {
        FrequencyPresetData presetData = FrequencyPresetData.get(playerInventory.player);
        for (int row = 0; row < PRESET_ROWS; row++) {
            for (int col = 0; col < PRESET_SLOTS_PER_ROW; col++) {
                final int r = row;
                final int c = col;
                int slotId = PRESET_SLOT_START + row * PRESET_SLOTS_PER_ROW + col;
                int x = PRESET_SLOT_X_START + col * PRESET_SLOT_SPACING_X;
                int y = PRESET_SLOT_Y_START + row * PRESET_SLOT_SPACING_Y;
                this.addSlot(new GhostRecipeSlot(slotId, x, y,
                    () -> presetData.getStack(r, c),
                    (id, stack) -> {
                        if (playerInventory.player.level().isClientSide()) {
                            presetData.setStack(r, c, stack);
                            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                new PresetSlotUpdatePayload(r, c, stack));
                        }
                    }
                ));
            }
        }
    }

    // ==================== Click handling ====================

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Frequency slots (0, 1)
        if (isFrequencySlot(slotId)) {
            handleFrequencySlotClick(slotId, button, clickType, player);
            return;
        }
        // Preset slots (2-9)
        if (handlePresetSlotClick(slotId, button, clickType, player)) return;
        super.clicked(slotId, button, clickType, player);
    }

    protected void handleFrequencySlotClick(int slotId, int button, ClickType clickType, Player player) {
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
    }

    protected boolean handlePresetSlotClick(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= PRESET_SLOT_START && slotId < PRESET_SLOT_START + PRESET_ROWS * PRESET_SLOTS_PER_ROW) {
            FrequencyPresetData presetData = FrequencyPresetData.get(player);
            int presetIndex = (slotId - PRESET_SLOT_START) / PRESET_SLOTS_PER_ROW;
            int slotIndex = (slotId - PRESET_SLOT_START) % PRESET_SLOTS_PER_ROW;

            // Right-click (button 1) or Q-throw: clear the slot
            if (button == 1 || clickType == ClickType.THROW) {
                presetData.setStack(presetIndex, slotIndex, ItemStack.EMPTY);
                this.getSlot(slotId).set(ItemStack.EMPTY);
                return true;
            }

            // Left-click (button 0): place carried item
            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                ItemStack placed = carried.copy();
                placed.setCount(1);
                presetData.setStack(presetIndex, slotIndex, placed);
                this.getSlot(slotId).set(placed);
            } else {
                // Carried is empty, clear the slot
                presetData.setStack(presetIndex, slotIndex, ItemStack.EMPTY);
                this.getSlot(slotId).set(ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    // ==================== Common overrides ====================

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * Get the preset data for this player (used by the GUI panel).
     */
    public FrequencyPresetData getPresetData() {
        return FrequencyPresetData.get(this.player);
    }

    public BlockPos getPos() {
        return this.pos;
    }
}