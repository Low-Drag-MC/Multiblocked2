package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.event.RecipeTypeEvent;
import com.lowdragmc.mbd2.api.recipe.event.RecipeUIEvent;
import com.lowdragmc.mbd2.client.renderer.custom.CustomRendererRegistry;
import com.lowdragmc.mbd2.common.machine.definition.config.event.*;
import com.lowdragmc.mbd2.integration.geckolib.MachineCustomKeyframeEvent;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.EventTargetType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.lowdragmc.mbd2.integration.kubejs.events.MBDMachineEvents.MBD_MACHINE_EVENTS;

/**
 * Client only KubeJS events. This interface must never be touched from common code: it reaches into
 * {@code Dist.CLIENT} classes, which a dedicated server refuses to load.
 */
public interface MBDClientEvents {
    Map<Class<? extends MachineEvent>, Function<MachineEvent, EventResult>> machineEventHandlers = new HashMap<>();
    Map<Class<? extends RecipeTypeEvent>, Function<RecipeTypeEvent, EventResult>> recipeTypeEventHandlers = new HashMap<>();

    /**
     * Client side registration events, as opposed to the per machine events that live in {@code MBDMachineEvents}.
     */
    EventGroup MBD_CLIENT_EVENTS = EventGroup.of("MBDClientEvents");

    EventHandler CUSTOM_RENDERERS = MBD_CLIENT_EVENTS.client("registerCustomRenderers",
            () -> CustomRendererRegistryEventJS.class);

    // Client events
    EventHandler CLIENT_TICK = registerMachineEvent("onClientTick",
            MachineClientTickEvent.class,
            MBDMachineEvents.MachineClientTickEventJS.class,
            MBDMachineEvents.MachineClientTickEventJS::new);

    EventHandler CUSTOM_DATA_UPDATE = registerMachineEvent("onCustomDataUpdate",
            MachineCustomDataUpdateEvent.class,
            MBDMachineEvents.MachineCustomDataUpdateEventJS.class,
            MBDMachineEvents.MachineCustomDataUpdateEventJS::new);

    EventHandler CUSTOM_KEYFRAME = createCustomKeyframeEvent();

    // Recipe events
    EventHandler RECIPE_UI = registerRecipeTypeEvent("onRecipeUI",
            RecipeUIEvent.class,
            MBDRecipeTypeEvents.RecipeUIEventJS.class,
            MBDRecipeTypeEvents.RecipeUIEventJS::new);

    static EventHandler createCustomKeyframeEvent() {
        if (MBD2.isGeckolibLoaded()) {
            return registerMachineEvent("onCustomKeyframe",
                    MachineCustomKeyframeEvent.class,
                    MBDMachineEvents.MachineCustomKeyframeEventJS.class,
                    MBDMachineEvents.MachineCustomKeyframeEventJS::new);
        }
        return null;
    }

    static void init() {
        // NO-OP
    }

    /**
     * Collect the renderers contributed by client scripts and swap them into the registry. Must only be called
     * after the {@code client} script manager finished reloading.
     */
    static void reloadCustomRenderers() {
        var event = new CustomRendererRegistryEventJS();
        CUSTOM_RENDERERS.post(event);
        CustomRendererRegistry.reload(event.getRenderers());
    }

    static <E extends MachineEvent> EventHandler registerMachineEvent(String name, Class<E> eventClass,
                                                                      Class<? extends MBDMachineEvents.MachineEventJS<E>> eventJSClass,
                                                                      Function<E, MBDMachineEvents.MachineEventJS<E>> eventJSFactory) {
        var handler = MBD_MACHINE_EVENTS.client(name, () -> eventJSClass).requiredTarget(EventTargetType.ID);
        machineEventHandlers.put(eventClass, event -> handler.post(eventJSFactory.apply((E) event), event.machine.getDefinition().id()));
        return handler;
    }

    static <E extends RecipeTypeEvent> EventHandler registerRecipeTypeEvent(String name, Class<? extends RecipeTypeEvent> eventClass,
                                                                            Class<? extends MBDRecipeTypeEvents.RecipeTypeEventJS<E>> eventJSClass,
                                                                            Function<E, MBDRecipeTypeEvents.RecipeTypeEventJS<E>> eventJSFactory) {
        var handler = MBDRecipeTypeEvents.MBD_RECIPE_TYPE_EVENTS.client(name, () -> eventJSClass).requiredTarget(EventTargetType.ID);
        recipeTypeEventHandlers.put(eventClass, event -> handler.post(eventJSFactory.apply((E) event), event.recipeType.getRegistryName()));
        return handler;
    }

    static EventResult postMachineEvent(MachineEvent machineEvent) {
        return Optional.ofNullable(machineEventHandlers.get(machineEvent.getClass())).map(handler -> handler.apply(machineEvent)).orElse(EventResult.PASS);
    }

    static EventResult postRecipeTypeEvent(RecipeTypeEvent recipeTypeEvent) {
        return Optional.ofNullable(recipeTypeEventHandlers.get(recipeTypeEvent.getClass())).map(handler -> handler.apply(recipeTypeEvent)).orElse(EventResult.PASS);
    }
}
