package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MBDMachineRegistryEventJS extends StartupEventJS {
    public final static Map<String, Supplier<? extends MBDMachineDefinition.Builder>> BUILDERS = new HashMap<>();
    private final Map<ResourceLocation, MBDMachineDefinition.Builder> machineBuilders = new HashMap<>();

    public MBDMachineDefinition.Builder create(String machineType, ResourceLocation machineID) {
        var builderCreator = BUILDERS.get(machineType);
        if (builderCreator == null) {
            throw new IllegalArgumentException("Unknown machine type: " + machineType);
        }
        var builder = builderCreator.get();
        builder.id(machineID);
        machineBuilders.put(machineID, builder);
        return builder;
    }

    public void removeMachine(ResourceLocation id) {
        machineBuilders.remove(id);
        MBDRegistries.MACHINE_DEFINITIONS.remove(id);
    }

    @Nullable
    public MBDMachineDefinition getMachine(ResourceLocation id) {
        return MBDRegistries.MACHINE_DEFINITIONS.get(id);
    }


    @Override
    protected void afterPosted(EventResult result) {
        machineBuilders.forEach((id, builder) -> MBDRegistries.MACHINE_DEFINITIONS.register(id, builder.build()));
    }
}
