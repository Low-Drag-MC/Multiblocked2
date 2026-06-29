package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDClientEvents;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDServerEvents;
import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.*;

@Getter
public class MachineEvent extends Event {
    public final MBDMachine machine;

    public MachineEvent(MBDMachine machine) {
        this.machine = machine;
    }

    public MachineEvent postCustomEvent() {
        // post to the graph events
        // todo blueprint
//        machine.getDefinition().machineEvents().postGraphEvent(this);
        // post to the KubeJS events
        postKubeJSEvent();
        return this;
    }

    public MachineEvent postKubeJSEvent() {
        // post to the KubeJS events
        if (MBD2.isKubeJSLoaded()) {
            try {
                if (LDLib2.isClient()) {
                    if (MBDServerEvents.postMachineEvent(this).interruptFalse() && this instanceof ICancellableEvent cancelable) {
                        cancelable.setCanceled(true);
                    } else if (MBDClientEvents.postMachineEvent(this).interruptFalse() && this instanceof ICancellableEvent cancelable) {
                        cancelable.setCanceled(true);
                    }
                } else {
                    if (MBDServerEvents.postMachineEvent(this).interruptFalse() && this instanceof ICancellableEvent cancelable) {
                        cancelable.setCanceled(true);
                    }
                }
            } catch (Exception e) {
                MBD2.LOGGER.error("Failed to post KubeJS event {}", this, e);
            }
        }
        return this;
    }
//
//    /**
//     * Get the exposed parameters for the given event class, it will detect all public fields with annotations in the class.
//     * <br>
//     * {@link GraphParameterGet} marked fields will be used to pass parameters to the graph.
//     * <br<
//     * {@link GraphParameterSet} marked fields will be used to gather parameters from the graph.
//     * @param clazz event class
//     * @return parameters
//     */
//    public static List<ExposedParameter<?>> getExposedParameters(Class<? extends MachineEvent> clazz) {
//        var parameters = new ArrayList<ExposedParameter<?>>();
//        for (var field : clazz.getFields()) {
//            if (field.isAnnotationPresent(GraphParameterGet.class)) {
//                var annotation = field.getAnnotation(GraphParameterGet.class);
//                var identity = field.getName();
//                var displayName = field.getName();
//                var type = field.getType();
//                List<String> tips = null;
//                if (!annotation.identity().isEmpty()) {
//                    identity = annotation.identity();
//                }
//                if (!annotation.displayName().isEmpty()) {
//                    displayName = annotation.displayName();
//                }
//                if (annotation.type() != ExposedParameter.class) {
//                    type = annotation.type();
//                }
//                if (annotation.tips().length > 0) {
//                    tips = Arrays.asList(annotation.tips());
//                }
//                parameters.add(new ExposedParameter<>(identity, type)
//                        .setTips(tips)
//                        .setAccessor(ExposedParameter.ParameterAccessor.Get)
//                        .setDisplayName(displayName));
//            }
//            if (field.isAnnotationPresent(GraphParameterSet.class)) {
//                var annotation = field.getAnnotation(GraphParameterSet.class);
//                var identity = field.getName();
//                var displayName = field.getName();
//                var type = field.getType();
//                List<String> tips = null;
//                if (!annotation.identity().isEmpty()) {
//                    identity = annotation.identity();
//                }
//                if (!annotation.displayName().isEmpty()) {
//                    displayName = annotation.displayName();
//                }
//                if (annotation.type() != ExposedParameter.class) {
//                    type = annotation.type();
//                }
//                if (annotation.tips().length > 0) {
//                    tips = Arrays.asList(annotation.tips());
//                }
//                parameters.add(new ExposedParameter<>(identity, type)
//                        .setTips(tips)
//                        .setAccessor(ExposedParameter.ParameterAccessor.Set)
//                        .setDisplayName(displayName));
//            }
//        }
//        if (clazz.isAnnotationPresent(Cancelable.class)) {
//            parameters.add(new ExposedParameter<>("cancel", Boolean.class)
//                    .setAccessor(ExposedParameter.ParameterAccessor.Set)
//                    .setDisplayName("cancel"));
//        }
//        return parameters;
//    }
//
//    /**
//     * Bind (pass) the parameters to the graph before the graph is processed.
//     */
//    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
//        Optional.ofNullable(exposedParameters.get("machine")).ifPresent(p -> p.setValue(machine));
//    }
//
//    /**
//     * Gather (get) the parameters from the graph after the graph has been processed.
//     */
//    public void gatherParameters(Map<String, ExposedParameter> exposedParameters) {
//        if (isCancelable()) {
//            Optional.ofNullable(exposedParameters.get("cancel")).ifPresent(p -> {
//                if (p.getValue() instanceof Boolean cancel) {
//                    setCanceled(cancel);
//                }
//            });
//        }
//    }

    @Override
    public String toString() {
        return "MachineEvent{" +
                "machine=" + machine +
                ", eventName='" + getClass().getSimpleName() + '\'' +
                (this instanceof ICancellableEvent event ?
                        ", isCanceled=" + event.isCanceled() : "") +
                '}';
    }
}
