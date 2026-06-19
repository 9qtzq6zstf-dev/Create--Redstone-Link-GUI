package com.ggrgg.createredstonelinkgui;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MOVE_RANGE = BUILDER
            .comment("Maximum distance in blocks a redstone link can be moved")
            .defineInRange("moveRange", 24, 1, 256);

    public enum ClickMode {
        SLOT,       // Right-click frequency slot with empty hand
        SHIFT_SLOT, // Shift + right-click frequency slot
        SHIFT_BLOCK // Shift + right-click anywhere on the block
    }

    public static final ModConfigSpec.EnumValue<ClickMode> CLICK_MODE = BUILDER
            .comment("How to open the redstone link frequency menu",
                     "SLOT - Right-click frequency slot with empty hand (default)",
                     "SHIFT_SLOT - Shift + right-click frequency slot",
                     "SHIFT_BLOCK - Shift + right-click anywhere on the block")
            .defineEnum("clickMode", ClickMode.SHIFT_SLOT);

    static final ModConfigSpec SPEC = BUILDER.build();
}