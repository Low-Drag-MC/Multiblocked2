package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.ILDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import lombok.Getter;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterSet;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.*;

@Getter
public class MachineEvent extends Event implements ILDLRegister {
    @GraphParameterGet
    public final MBDMachine machine;

    public MachineEvent(MBDMachine machine) {
        this.machine = machine;
    }

    public MachineEvent postCustomEvent() {
        // post to the graph events
        machine.getDefinition().machineEvents().postGraphEvent(this);
        // post to the KubeJS events
        // TODO
        return this;
    }

    /**
     * Get the exposed parameters for the given event class, it will detect all public fields with annotations in the class.
     * <br>
     * {@link GraphParameterGet} marked fields will be used to pass parameters to the graph.
     * <br<
     * {@link GraphParameterSet} marked fields will be used to gather parameters from the graph.
     * @param clazz event class
     * @return parameters
     */
    public static List<ExposedParameter<?>> getExposedParameters(Class<? extends MachineEvent> clazz) {
        var parameters = new ArrayList<ExposedParameter<?>>();
        for (var field : clazz.getFields()) {
            if (field.isAnnotationPresent(GraphParameterGet.class)) {
                var annotation = field.getAnnotation(GraphParameterGet.class);
                var displayName = field.getName();
                var type = field.getType();
                List<String> tips = null;
                if (!annotation.displayName().isEmpty()) {
                    displayName = annotation.displayName();
                }
                if (annotation.type() != ExposedParameter.class) {
                    type = annotation.type();
                }
                if (annotation.tips().length > 0) {
                    tips = Arrays.asList(annotation.tips());
                }
                parameters.add(new ExposedParameter<>(field.getName(), type)
                        .setTips(tips)
                        .setAccessor(ExposedParameter.ParameterAccessor.Get)
                        .setDisplayName(displayName));
            }
            if (field.isAnnotationPresent(GraphParameterSet.class)) {
                var annotation = field.getAnnotation(GraphParameterSet.class);
                var displayName = field.getName();
                var type = field.getType();
                List<String> tips = null;
                if (!annotation.displayName().isEmpty()) {
                    displayName = annotation.displayName();
                }
                if (annotation.type() != ExposedParameter.class) {
                    type = annotation.type();
                }
                if (annotation.tips().length > 0) {
                    tips = Arrays.asList(annotation.tips());
                }
                parameters.add(new ExposedParameter<>(field.getName(), type)
                        .setTips(tips)
                        .setAccessor(ExposedParameter.ParameterAccessor.Set)
                        .setDisplayName(displayName));
            }
        }
        if (clazz.isAnnotationPresent(Cancelable.class)) {
            parameters.add(new ExposedParameter<>("cancel", Boolean.class)
                    .setAccessor(ExposedParameter.ParameterAccessor.Set)
                    .setDisplayName("cancel"));
        }
        return parameters;
    }

    /**
     * Bind (pass) the parameters to the graph before the graph is processed.
     */
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        Optional.ofNullable(exposedParameters.get("machine")).ifPresent(p -> p.setValue(machine));
    }

    /**
     * Gather (get) the parameters from the graph after the graph has been processed.
     */
    public void gatherParameters(Map<String, ExposedParameter> exposedParameters) {
        if (isCancelable()) {
            Optional.ofNullable(exposedParameters.get("cancel")).ifPresent(p -> {
                if (p.getValue() instanceof Boolean cancel) {
                    setCanceled(cancel);
                }
            });
        }
    }

}
