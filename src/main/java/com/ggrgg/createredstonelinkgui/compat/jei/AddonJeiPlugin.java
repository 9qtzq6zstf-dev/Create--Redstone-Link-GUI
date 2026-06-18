package com.ggrgg.createredstonelinkgui.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;
import com.ggrgg.createredstonelinkgui.client.screen.VoidLinkConfigScreen;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class AddonJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath("createredstonelinkgui", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(RedstoneLinkConfigScreen.class, new RedstoneLinkGhostHandler());
        registration.addGhostIngredientHandler(VoidLinkConfigScreen.class, new VoidLinkGhostHandler());
        registration.addGenericGuiContainerHandler(RedstoneLinkConfigScreen.class, new IGuiContainerHandler<RedstoneLinkConfigScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(RedstoneLinkConfigScreen screen) {
                return screen.blockPreviewBounds != null
                    ? List.of(screen.blockPreviewBounds)
                    : List.of();
            }
        });
    }

    private static class VoidLinkGhostHandler implements IGhostIngredientHandler<VoidLinkConfigScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(VoidLinkConfigScreen screen, ITypedIngredient<I> typed, boolean start) {
            var stack = typed.getIngredient(VanillaTypes.ITEM_STACK);
            if (stack.isEmpty()) return List.of();
            ItemStack item = stack.get();
            List<Target<I>> targets = new ArrayList<>(2);
            targets.add(new Target<>() {
                @Override public Rect2i getArea() { return screen.slot1Bounds; }
                @Override public void accept(I ing) { screen.updateFrequencySlot(0, item); }
            });
            targets.add(new Target<>() {
                @Override public Rect2i getArea() { return screen.slot2Bounds; }
                @Override public void accept(I ing) { screen.updateFrequencySlot(1, item); }
            });
            return targets;
        }
        @Override public void onComplete() {}
    }
}