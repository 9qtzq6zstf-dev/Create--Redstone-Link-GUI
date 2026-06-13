package com.ggrgg.createredstonelinkgui;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MOVE_RANGE = BUILDER
            .comment("Maximum distance in blocks a redstone link can be moved")
            .defineInRange("moveRange", 24, 1, 256);

    static final ModConfigSpec SPEC = BUILDER.build();
}