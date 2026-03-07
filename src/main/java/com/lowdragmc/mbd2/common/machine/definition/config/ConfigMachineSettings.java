package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import lombok.*;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;


import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Builder
public class ConfigMachineSettings implements IPersistedSerializable, IConfigurable {
    @Getter @Setter
    public static class SignalConnection {
        @Configurable(name = "config.machine_settings.signal_connection.front")
        private boolean frontConnection = false;
        @Configurable(name = "config.machine_settings.signal_connection.back")
        private boolean backConnection = false;
        @Configurable(name = "config.machine_settings.signal_connection.left")
        private boolean leftConnection = false;
        @Configurable(name = "config.machine_settings.signal_connection.right")
        private boolean rightConnection = false;
        @Configurable(name = "config.machine_settings.signal_connection.top")
        private boolean topConnection = false;
        @Configurable(name = "config.machine_settings.signal_connection.bottom")
        private boolean bottomConnection = false;

        public boolean getConnection(Direction front, Direction side) {
            if (side == Direction.UP) {
                return topConnection;
            } else if (side == Direction.DOWN) {
                return bottomConnection;
            } else if (side == front) {
                return frontConnection;
            } else if (side == front.getOpposite()) {
                return backConnection;
            } else if (side == front.getClockWise()) {
                return rightConnection;
            } else if (side == front.getCounterClockWise()) {
                return leftConnection;
            }
            return false;
        }
    }
    @Getter
    @Builder.Default
    @Configurable(name = "config.machine_settings.machine_level", tips = "config.machine_settings.machine_level.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int machineLevel = 0;
    @Getter
    @Builder.Default
    @Configurable(name = "config.machine_settings.has_ui", tips = "config.machine_settings.has_ui.tooltip")
    private boolean hasUI = true;
    @Getter
    @Builder.Default
    @Configurable(name = "config.machine_settings.drop_machine_item", tips = {
            "config.machine_settings.drop_machine_item.tooltip.0",
            "config.machine_settings.drop_machine_item.tooltip.1",
            "config.machine_settings.drop_machine_item.tooltip.2",
    })
    private boolean dropMachineItem = true;
    @Getter
    @Builder.Default
    @Configurable(name = "config.machine_settings.signal_connection", subConfigurable = true,
            tips = {"config.machine_settings.signal_connection.tooltip.0", "config.machine_settings.signal_connection.tooltip.1"})
    private final SignalConnection signalConnection = new SignalConnection();
    @Singular
    @Persisted
    @Getter
    private List<TraitDefinition> traitDefinitions;

    public void addTraitDefinition(TraitDefinition definition) {
        traitDefinitions = new ArrayList<>(traitDefinitions);
        traitDefinitions.add(definition);
    }

    public void removeTraitDefinition(TraitDefinition definition) {
        traitDefinitions = this.traitDefinitions.stream().filter(s -> s != definition).toList();
    }

    public void moveTraitDefinition(TraitDefinition toMoved, int newIndex) {
        if (toMoved == null || traitDefinitions == null || traitDefinitions.isEmpty()) {
            return;
        }
        var definitions = new ArrayList<>(traitDefinitions);
        int oldIndex = definitions.indexOf(toMoved);
        if (oldIndex < 0) {
            return;
        }
        int boundedIndex = Math.max(0, Math.min(newIndex, definitions.size()));
        if (oldIndex < boundedIndex) {
            boundedIndex--;
        }
        if (oldIndex == boundedIndex) {
            return;
        }
        definitions.remove(oldIndex);
        definitions.add(boundedIndex, toMoved);
        traitDefinitions = definitions;
    }
}
