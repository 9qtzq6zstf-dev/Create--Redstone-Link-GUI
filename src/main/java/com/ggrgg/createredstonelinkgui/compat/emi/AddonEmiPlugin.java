package com.ggrgg.createredstonelinkgui.compat.emi;

import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class AddonEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(RedstoneLinkConfigScreen.class, new RedstoneLinkEmiDragHandler());
    }
}
