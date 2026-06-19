package com.ggrgg.createredstonelinkgui;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public enum ClickMode {
        SLOT,       // Right-click frequency slot with empty hand
        SHIFT_SLOT, // Shift + right-click frequency slot
        SHIFT_BLOCK // Shift + right-click anywhere on the block
    }

    public static final ModConfigSpec.EnumValue<ClickMode> CLICK_MODE = BUILDER
            .comment("How to open the redstone link frequency menu (Client-side)",
                     "SLOT - Right-click frequency slot with empty hand (default)",
                     "SHIFT_SLOT - Shift + right-click frequency slot",
                     "SHIFT_BLOCK - Shift + right-click anywhere on the block")
            .defineEnum("clickMode", ClickMode.SHIFT_SLOT);

    static final ModConfigSpec SPEC = BUILDER.build();
}