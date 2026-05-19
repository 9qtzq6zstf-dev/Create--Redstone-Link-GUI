package com.ggrgg.createredstonelinkgui;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreateRedstoneLinkGUI.MODID)
public class CreateRedstoneLinkGUI {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "createredstonelinkgui";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public CreateRedstoneLinkGUI(IEventBus modEventBus, ModContainer modContainer) {
        // Register core network configurations
        modEventBus.addListener(this::registerPackets);
        
        // Setup deferred registry items
        RedstoneLinkMenu.MENUS.register(modEventBus);
        
        // Client environment isolation check to block headless server errors
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerScreens);
        }

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToServer(
                RedstoneLinkFrequencyPayload.TYPE,
                RedstoneLinkFrequencyPayload.CODEC,
                RedstoneLinkFrequencyPayload::handleServer
        );
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(RedstoneLinkMenu.TYPE.get(), RedstoneLinkConfigScreen::new);
    }
}
