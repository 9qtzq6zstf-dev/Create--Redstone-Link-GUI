package com.ggrgg.createredstonelinkgui.compat.emi;

import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@EmiEntrypoint
public class AddonEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(RedstoneLinkConfigScreen.class, new RedstoneLinkEmiDragHandler());
        registry.addDragDropHandler(com.ggrgg.createredstonelinkgui.client.screen.VoidLinkConfigScreen.class, new VoidLinkEmiDragHandler());
        registry.addExclusionArea(RedstoneLinkConfigScreen.class, (screen, consumer) -> {
            if (screen.blockPreviewBounds != null)
                consumer.accept(new Bounds(
                    screen.blockPreviewBounds.getX(),
                    screen.blockPreviewBounds.getY(),
                    screen.blockPreviewBounds.getWidth(),
                    screen.blockPreviewBounds.getHeight()
                ));
        });
        registry.addExclusionArea(com.ggrgg.createredstonelinkgui.client.screen.VoidLinkConfigScreen.class, (screen, consumer) -> {
            if (screen.blockPreviewBounds != null)
                consumer.accept(new Bounds(
                    screen.blockPreviewBounds.getX(),
                    screen.blockPreviewBounds.getY(),
                    screen.blockPreviewBounds.getWidth(),
                    screen.blockPreviewBounds.getHeight()
                ));
        });

        // Unhide frequency mod symbol items that are hidden from creative tabs
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!id.getNamespace().equals("frequency")) continue;
            String path = id.getPath();
            if (!path.startsWith("symbol_")) continue;
            if (path.equals("symbol_frame")) continue;

            registry.addEmiStack(EmiStack.of(new ItemStack(entry.getValue())));
        }
    }
}