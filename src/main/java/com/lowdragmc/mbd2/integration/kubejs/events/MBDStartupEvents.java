package com.lowdragmc.mbd2.integration.kubejs.events;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface MBDStartupEvents {
    EventGroup REGISTRY_EVENTS = EventGroup.of("MBDRegistryEvents");
    EventHandler MACHINE = REGISTRY_EVENTS.startup("machine", () -> MBDMachineRegistryEventJS.class);
    EventHandler RECIPE_TYPE = REGISTRY_EVENTS.startup("recipeType", () -> MBDRecipeTypeRegistryEventJS.class);
}
