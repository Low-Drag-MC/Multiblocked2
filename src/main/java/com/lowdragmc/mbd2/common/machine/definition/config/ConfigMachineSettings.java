package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.mbd2.common.gui.MBDBindingIDs;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import lombok.*;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.IntTag;


import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    @Builder.Default
    @Persisted
    private UITemplate uiTemplate = null;
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
    @Builder.Default
    @Persisted
    @Getter
    private final List<TraitDefinition> traitDefinitions = new ArrayList<>();
    /**
     * Blueprints attached to this machine, in execution order.
     *
     * <p>Ordered because the list is a behaviour pipeline: for a value-modifying event each blueprint
     * sees the previous one's result, and for a cancelable event any blueprint cancelling wins. See
     * {@link com.lowdragmc.mbd2.common.machine.MBDMachine#postBlueprintEvent}.</p>
     */
    @Builder.Default
    @Getter
    @Configurable(name = "config.machine_settings.blueprints", tips = {
            "config.machine_settings.blueprints.tooltip.0",
            "config.machine_settings.blueprints.tooltip.1",
    })
    @ConfigList(configuratorMethod = "blueprintConfigurator", addDefaultMethod = "defaultBlueprint")
    @ReadOnlyManaged(serializeMethod = "blueprintsSerialize", deserializeMethod = "blueprintsDeserialize")
    private final List<MachineBlueprintBinding> blueprints = new ArrayList<>();

    // The list instance is final, so the persisted form has to say how many elements to create before
    // per-element deserialization can run. Same shape as RecipeModifier.RecipeModifiers.
    protected IntTag blueprintsSerialize(List<MachineBlueprintBinding> bindings) {
        return IntTag.valueOf(bindings.size());
    }

    protected List<MachineBlueprintBinding> blueprintsDeserialize(IntTag tag) {
        var bindings = new ArrayList<MachineBlueprintBinding>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            bindings.add(defaultBlueprint());
        }
        return bindings;
    }

    protected Configurator blueprintConfigurator(Supplier<MachineBlueprintBinding> getter,
                                                 Consumer<MachineBlueprintBinding> setter) {
        var group = new ConfiguratorGroup("", false).hideTitle();
        getter.get().buildConfigurator(group);
        return group;
    }

    protected MachineBlueprintBinding defaultBlueprint() {
        return new MachineBlueprintBinding();
    }

    public boolean canAddTraitDefinition(TraitDefinition definition) {
        return definition.canBeAddedTo(traitDefinitions);
    }

    public boolean addTraitDefinition(TraitDefinition definition) {
        if (!canAddTraitDefinition(definition)) {
            return false;
        }
        traitDefinitions.add(definition);
        return true;
    }

    public void removeTraitDefinition(TraitDefinition definition) {
        traitDefinitions.removeIf(s -> s == definition);
    }

    public void moveTraitDefinition(@Nullable TraitDefinition toMoved, int newIndex) {
        if (toMoved == null || traitDefinitions.isEmpty()) {
            return;
        }
        int oldIndex = traitDefinitions.indexOf(toMoved);
        if (oldIndex < 0) {
            return;
        }
        int boundedIndex = Math.clamp(newIndex, 0, traitDefinitions.size());
        if (oldIndex < boundedIndex) {
            boundedIndex--;
        }
        if (oldIndex == boundedIndex) {
            return;
        }
        traitDefinitions.remove(oldIndex);
        traitDefinitions.add(boundedIndex, toMoved);
    }

    public UITemplate uiTemplate() {
        if (uiTemplate == null) {
            uiTemplate = UITemplate.of(new UIElement()
                    .addChildren(
                            new Label().setText("Machine UI").setId(MBDBindingIDs.MACHINE_NAME),
                            new UIElement().layout(l -> l.height(40)),
                            new InventorySlots()
                    )
                    .addClass("panel_bg"), StylesheetManager.MC_MERGED);
        }
        return uiTemplate;
    }
}
