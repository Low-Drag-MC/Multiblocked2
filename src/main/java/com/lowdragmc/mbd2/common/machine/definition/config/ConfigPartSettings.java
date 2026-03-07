package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.trait.CapabilityIO;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.IntTag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@Builder
@Accessors(fluent = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigPartSettings implements IToggleConfigurable {
    @Getter
    @Setter
    protected boolean enable;

    @Override
    public boolean isEnable() {
        return enable;
    }

    @Override
    public void setEnable(boolean enable) {
        // todo machine state?
//        if (enable && !this.enable && LDLib2.isClient() && Editor.INSTANCE instanceof MachineEditor machineEditor
//                && machineEditor.getCurrentProject() instanceof MachineProject project) {
//            // ask to add the "formed" state
//            if (!project.getDefinition().stateMachine().hasState("formed")) {
//                DialogWidget.showCheckBox(Editor.INSTANCE, "config.part_settings.formed_tips", "config.part_settings.formed_tips.info", result -> {
//                    if (result) {
//                        var state = project.getDefinition().stateMachine().getRootState();
//                        var newState = state.addChild("formed");
//                        machineEditor.getTabPages().tabs.values().stream()
//                                .filter(MachineConfigPanel.class::isInstance)
//                                .map(MachineConfigPanel.class::cast)
//                                .findAny().ifPresent(panel -> panel.onStateAdded(newState));
//                    }
//                });
//            }
//        }
        this.enable = enable;
    }

    @Configurable(name = "config.part_settings.can_share", tips = {"config.part_settings.can_share.tooltip"})
    @Builder.Default
    protected boolean canShare = true;
    @Builder.Default
    @Configurable(subConfigurable = true)
    protected final RecipeModifier.RecipeModifiers recipeModifiers = new RecipeModifier.RecipeModifiers();
    @Builder.Default
    @Configurable(name = "config.part_settings.proxy_controller_capabilities")
    @ConfigList(configuratorMethod = "proxyCapabilitiesConfigurator", addDefaultMethod = "defaultProxyCapabilities")
    @ReadOnlyManaged(serializeMethod = "proxyCapabilitiesSerialize", deserializeMethod = "proxyCapabilitiesDeserialize")
    protected final List<ProxyCapability> proxyControllerCapabilities = new ArrayList<>();

    protected Configurator proxyCapabilitiesConfigurator(Supplier<ProxyCapability> getter, Consumer<ProxyCapability> setter) {
        var group = new ConfiguratorGroup("", false).hideTitle();
        getter.get().buildConfigurator(group);
        return group;
    }

    protected ProxyCapability defaultProxyCapabilities() {
        return new ProxyCapability();
    }

    protected IntTag proxyCapabilitiesSerialize(List<ProxyCapability> list) {
        return IntTag.valueOf(list.size());
    }

    protected List<ProxyCapability> proxyCapabilitiesDeserialize(IntTag tag) {
        var list = new ArrayList<ProxyCapability>(tag.getAsInt());
        for (int i = 0; i < tag.getAsInt(); i++) {
            list.add(defaultProxyCapabilities());
        }
        return list;
    }
    /**
     * To proxy the capabilities from the controller.
     */
    @Getter
    public static class ProxyCapability implements IConfigurable, IPersistedSerializable {
        @Configurable(name = "config.part_settings.proxy_capability.trait_name_filter",
                tips = {"config.part_settings.proxy_capability.trait_name_filter.tooltip.0",
                        "config.part_settings.proxy_capability.trait_name_filter.tooltip.1"})
        @Setter
        private String traitNameFilter;
        @Configurable(name = "config.definition.trait.capability_io", subConfigurable = true,
                tips = {"config.definition.trait.capability_io.tooltip.0", "config.definition.trait.capability_io.tooltip.1"})
        private final CapabilityIO capabilityIO = new CapabilityIO();
        @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true,
                tips = {"config.definition.trait.auto_io.tooltip"})
        private final ToggleAutoIO autoIO = new ToggleAutoIO();
    }
}
