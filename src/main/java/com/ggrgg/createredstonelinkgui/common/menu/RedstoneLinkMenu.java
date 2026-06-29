package com.ggrgg.createredstonelinkgui.common.menu;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RedstoneLinkMenu extends AbstractLinkMenu {
    
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "createredstonelinkgui");
    
    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneLinkMenu>> TYPE = MENUS.register("redstone_link_menu", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new RedstoneLinkMenu(windowId, inv, data.readBlockPos()))
    );

    private LinkBehaviour behaviour;
    private boolean isRedstoneLink;
    private boolean receiverMode;

    public LinkBehaviour getBehaviourTyped() { return this.behaviour; }

    @Override
    public Object getBehaviour() { return this.behaviour; }

    public boolean isRedstoneLink() { return this.isRedstoneLink; }
    public boolean isReceiverMode() { return this.receiverMode; }

    public RedstoneLinkMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(containerId, playerInventory, pos, TYPE.get());
        
        var level = playerInventory.player.level();
        var state = level.getBlockState(pos);
        this.isRedstoneLink = state.getBlock() instanceof RedstoneLinkBlock;
        this.receiverMode = this.isRedstoneLink && state.getValue(RedstoneLinkBlock.RECEIVER);
        
        var be = level.getBlockEntity(pos);
        if (be != null) {
            this.behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
        }

        // 1. Frequency slots FIRST — indices 0, 1
        this.addSlot(new GhostRecipeSlot(0, 101, 34,
            () -> FrequencyHelper.getFrequencyItem(behaviour, 0),
            (id, stack) -> FrequencyHelper.setFrequencyItem(behaviour, id, stack)));
        this.addSlot(new GhostRecipeSlot(1, 137, 34,
            () -> FrequencyHelper.getFrequencyItem(behaviour, 1),
            (id, stack) -> FrequencyHelper.setFrequencyItem(behaviour, id, stack)));

        // 2. Preset slots at indices 2-9
        addPresetSlots(playerInventory);

        // 3. Player inventory at indices 10+
        addPlayerInventorySlots(playerInventory);
    }
}