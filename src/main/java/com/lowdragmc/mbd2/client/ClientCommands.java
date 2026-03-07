package com.lowdragmc.mbd2.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientCommands {

    public static List<LiteralArgumentBuilder<CommandSourceStack>> createClientCommands() {
        return List.of();
    }

}
