package com.ggrgg.createredstonelinkgui.compat.emi;

import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;

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
    }
}