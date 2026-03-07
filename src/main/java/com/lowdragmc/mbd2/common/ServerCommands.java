package com.lowdragmc.mbd2.common;

import com.lowdragmc.lowdraglib2.gui.editor.UIEditor;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor;
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
                        .requires(s -> s.getServer().isSingleplayer() && s.hasPermission(2))
                        .then(Commands.literal("reload_machine_projects")
                                .executes(context -> {
                                    // clear up the catalyst candidates
                                    MultiblockMachineDefinition.CATALYST_CANDIDATES.clear();
                                    // reload all machine definitions
                                    for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                                        if (definition.isCreatedFromProjectFile()) {
                                            definition.reloadFromProjectFile();
                                            context.getSource().sendSystemMessage(Component.literal(definition.id().toString()).append(Component.translatable("project.reload")));
                                        }
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("reload_recipe_type_projects")
                                .executes(context -> {
                                    // reload all recipe types
                                    for (var recipeType : MBDRegistries.RECIPE_TYPES) {
                                        if (recipeType.isCreatedFromProjectFile()) {
                                            recipeType.reloadFromProjectFile();
                                            context.getSource().sendSystemMessage(Component.literal(recipeType.getRegistryName().toString()).append(Component.translatable("project.reload")));
                                            context.getSource().sendSystemMessage(Component.translatable("project.reload.recipe"));
                                        }
                                    }
                                    return 1;
                                })
                        ),
                Commands.literal("mbd2_editor")
                        .requires(s -> s.getServer().isSingleplayer() && s.hasPermission(2))
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) return 0;
                            PlayerUIMenuType.openUI(context.getSource().getPlayer(), MBDEditor.WINDOW_ID);
                            return 1;
                        })
        );
    }
}
