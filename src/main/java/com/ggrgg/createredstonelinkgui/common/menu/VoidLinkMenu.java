package com.ggrgg.createredstonelinkgui.common.menu;

import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VoidLinkMenu extends AbstractLinkMenu {
    
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "createredstonelinkgui");
    
    public static final DeferredHolder<MenuType<?>, MenuType<VoidLinkMenu>> TYPE = MENUS.register("void_link_menu", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new VoidLinkMenu(windowId, inv, data.readBlockPos()))
    );

    private Object behaviour;

    @Override
    public Object getBehaviour() { return this.behaviour; }

    public Object getBehaviourRaw() { return this.behaviour; }

    public VoidLinkMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(containerId, playerInventory, pos, TYPE.get());
        
        var level = playerInventory.player.level();
        this.behaviour = VoidLinkHelper.getBehaviour(level, pos);

        // Add custom Ghost Recipe Slots for frequency slots
        this.addSlot(new GhostRecipeSlot(0, 101, 34, () -> FrequencyHelper.getFrequencyItem(behaviour, 0),
            (id, stack) -> FrequencyHelper.setFrequencyItem(behaviour, id, stack)));
        this.addSlot(new GhostRecipeSlot(1, 137, 34, () -> FrequencyHelper.getFrequencyItem(behaviour, 1),
            (id, stack) -> FrequencyHelper.setFrequencyItem(behaviour, id, stack)));
    }
}