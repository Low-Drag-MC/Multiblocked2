package com.lowdragmc.mbd2.common;

import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ServerCommands
 */
public class ServerCommands {
    public static List<LiteralArgumentBuilder<CommandSourceStack>> createServerCommands() {
        return List.of(
                Commands.literal("mbd2")
                        .then(Commands.literal("reload_machine_projects")
                                .executes(context -> {
                                    // clear up the catalyst candidates
                                    MultiblockMachineDefinition.CATALYST_CANDIDATES.clear();
                                    // reload all machine definitions
                                    for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                                        if (definition.isCreatedFromProjectFile()) {
                                            definition.reloadFromProjectFile();
                                            context.getSource().sendSystemMessage(Component.literal(definition.projectFile().getPath() + " reloaded"));
                                        }
                                    }
                                    return 1;
                                })
                        )
        );
    }
}
