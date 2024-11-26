package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineConfigPanel;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
@Builder
@Accessors(fluent = true)
public class ConfigPartSettings implements IToggleConfigurable, IPersistedSerializable {

    @Getter
    @Setter
    @Persisted
    protected boolean enable;

    @Override
    public boolean isEnable() {
        return enable;
    }

    @Override
    public void setEnable(boolean enable) {
        if (enable && !this.enable && LDLib.isClient() && Editor.INSTANCE instanceof MachineEditor machineEditor
                && machineEditor.getCurrentProject() instanceof MachineProject project) {
            // ask to add the "formed" state
            if (!project.getDefinition().stateMachine().hasState("formed")) {
                DialogWidget.showCheckBox(Editor.INSTANCE, "config.part_settings.formed_tips", "config.part_settings.formed_tips.info", result -> {
                    if (result) {
                        var state = project.getDefinition().stateMachine().getRootState();
                        var newState = state.addChild("formed");
                        machineEditor.getTabPages().tabs.values().stream()
                                .filter(MachineConfigPanel.class::isInstance)
                                .map(MachineConfigPanel.class::cast)
                                .findAny().ifPresent(panel -> panel.onStateAdded(newState));
                    }
                });
            }
        }
        this.enable = enable;
    }

    @Configurable(name = "config.part_settings.can_share", tips = {"config.part_settings.can_share.tooltip"})
    @Builder.Default
    protected boolean canShare = true;
    @Builder.Default
    protected final RecipeModifier.RecipeModifiers recipeModifiers = new RecipeModifier.RecipeModifiers();
    @Builder.Default
    protected final List<ProxyCapability> proxyControllerCapabilities = new ArrayList<>();
    @Builder.Default
    protected final List<ExtraTraitAction> extraTraitActions = new ArrayList<>();

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        tag.put("recipeModifiers", recipeModifiers.serializeNBT());
        var proxyCapabilities = new ListTag();
        for (ProxyCapability proxyCapability : proxyControllerCapabilities) {
            proxyCapabilities.add(proxyCapability.serializeNBT());
        }
        var traitActions = new ListTag();
        for (ExtraTraitAction extraTraitAction : extraTraitActions) {
            traitActions.add(extraTraitAction.serializeNBT());
        }
        tag.put("proxyControllerCapabilities", proxyCapabilities);
        tag.put("extraTraitActions", traitActions);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(tag);
        recipeModifiers.deserializeNBT(tag.getList("recipeModifiers", Tag.TAG_COMPOUND));
        proxyControllerCapabilities.clear();
        var proxyCapabilities = tag.getList("proxyControllerCapabilities", Tag.TAG_COMPOUND);
        for (int i = 0; i < proxyCapabilities.size(); i++) {
            var proxyCapability = new ProxyCapability();
            proxyCapability.deserializeNBT(proxyCapabilities.getCompound(i));
            proxyControllerCapabilities.add(proxyCapability);
        }
        var traitActions = tag.getList("extraTraitActions", Tag.TAG_COMPOUND);
        for (int i = 0; i < traitActions.size(); i++) {
            var extraTraitAction = new ExtraTraitAction();
            extraTraitAction.deserializeNBT(traitActions.getCompound(i));
            extraTraitActions.add(extraTraitAction);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IToggleConfigurable.super.buildConfigurator(father);
        recipeModifiers.buildConfigurator(father);
        var proxyCapabilities = new ArrayConfiguratorGroup<>("config.part_settings.proxy_controller_capabilities", false,
                                                             () -> proxyControllerCapabilities, (getter, setter) -> {
            var proxyCapability = getter.get();
            var group = new ConfiguratorGroup("config.part_settings.proxy_capability.trait_filter", false);
            proxyCapability.buildConfigurator(group);
            return group;
        }, true
        );
        proxyCapabilities.setTips("config.part_settings.proxy_controller_capabilities.tooltip");
        proxyCapabilities.setAddDefault(ProxyCapability::new);
        proxyCapabilities.setOnAdd(proxyControllerCapabilities::add);
        proxyCapabilities.setOnRemove(proxyControllerCapabilities::remove);
        proxyCapabilities.setOnUpdate(list -> {
            proxyControllerCapabilities.clear();
            proxyControllerCapabilities.addAll(list);
        });
        father.addConfigurators(proxyCapabilities);
        var traitActions = new ArrayConfiguratorGroup<>("config.part_settings.extra_trait_actions", false,
                                                        () -> this.extraTraitActions, (getter, setter) -> {
            var extraTraitAction = getter.get();
            var group = new ConfiguratorGroup("config.part_settings.extra_trait_action.trait_io", false);
            extraTraitAction.buildConfigurator(group);
            return group;
        }, true
        );
        traitActions.setTips("config.part_settings.extra_trait_actions.tooltip");
        traitActions.setAddDefault(ExtraTraitAction::new);
        traitActions.setOnAdd(extraTraitActions::add);
        traitActions.setOnRemove(extraTraitActions::remove);
        traitActions.setOnUpdate(list -> {
            extraTraitActions.clear();
            extraTraitActions.addAll(list);
        });
        father.addConfigurators(traitActions);
    }

    /**
     * To proxy the capabilities from the controller.
     */
    @Getter
    public static class ProxyCapability implements IConfigurable, IPersistedSerializable {
        @Configurable(name = "config.part_settings.proxy_capability.trait_name_filter",
                tips = {"config.part_settings.proxy_capability.trait_name_filter.tooltip.0",
                        "config.part_settings.proxy_capability.trait_name_filter.tooltip.1"})
        private String traitNameFilter;
        @Configurable(name = "config.definition.trait.capability_io", subConfigurable = true,
                tips = {"config.definition.trait.capability_io.tooltip.0", "config.definition.trait.capability_io.tooltip.1"})
        private final SimpleCapabilityTraitDefinition.CapabilityIO capabilityIO = new SimpleCapabilityTraitDefinition.CapabilityIO();
    }

    public static class ExtraTraitAction implements IConfigurable, IPersistedSerializable {
        @Configurable(name = "config.part_settings.extra_trait_action.trait_name_filter",
                tips = {"config.part_settings.extra_trait_action.trait_name_filter.tooltip.0",
                        "config.part_settings.extra_trait_action.trait_name_filter.tooltip.1"})
        @Getter
        private String traitNameFilter;

        @Configurable(name = "config.part_settings.extra_trait_action.interval", tips = "config.part_settings.extra_trait_action.interval.tooltip")
        @NumberRange(range = {1, Integer.MAX_VALUE})
        @Getter
        private int interval = 20;

        private Pattern compiled;

        public Pattern getFilter() {
            if (compiled == null) {
                compiled = Pattern.compile(traitNameFilter);
            }
            return compiled;
        }

        @Configurable(name = "config.part_settings.extra_trait_action.action_io", subConfigurable = true,
                tips = {"config.part_settings.extra_trait_action.action.tooltip.0",
                        "config.part_settings.extra_trait_action.action.tooltip.1"})
        @Getter
        private final PartCapabilityIO capabilityIO = new PartCapabilityIO();
    }

    @Getter
    @Setter
    public static class PartCapabilityIO {
        @Configurable(name = "config.definition.trait.capability_io.front")
        private IO frontIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.back")
        private IO backIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.left")
        private IO leftIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.right")
        private IO rightIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.top")
        private IO topIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.bottom")
        private IO bottomIO = IO.BOTH;

        public IO getIO(Direction front, @Nullable Direction side) {
            if (side == Direction.UP) {
                return topIO;
            } else if (side == Direction.DOWN) {
                return bottomIO;
            } else if (side == front) {
                return frontIO;
            } else if (side == front.getOpposite()) {
                return backIO;
            } else if (side == front.getClockWise()) {
                return rightIO;
            } else if (side == front.getCounterClockWise()) {
                return leftIO;
            }
            return IO.NONE;
        }
    }
}