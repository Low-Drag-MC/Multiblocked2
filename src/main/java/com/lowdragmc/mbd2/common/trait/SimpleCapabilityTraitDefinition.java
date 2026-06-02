package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public abstract class SimpleCapabilityTraitDefinition<T, C extends @Nullable Object> extends RecipeCapabilityTraitDefinition implements IUIProviderTrait {
    public abstract static class Type<T, C extends @Nullable Object, D extends SimpleCapabilityTraitDefinition<T, C>> extends TraitDefinitionType<D> {
        protected Type(String name, String group) {
            super(name, group);
        }

        protected Type(String name) {
            super(name);
        }

        @Override
        public void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(
                    getCapability(),
                    definition.blockEntityType(),
                    (be, context) -> {
                        if (be instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                            T own = getCapContent(machine, context);
                            if (machine instanceof MBDPartMachine part) {
                                T proxied = getProxiedCapContent(part, context);
                                return mergeOpt(own, proxied);
                            }
                            return own;
                        }
                        return null;
                    });
        }

        @Override
        @SuppressWarnings("unchecked")
        public void registerGlobalCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(
                    getCapability(),
                    (net.minecraft.world.level.block.entity.BlockEntityType<ProxyPartBlockEntity>) ProxyPartBlockEntity.TYPE(),
                    (be, context) -> getProxyPartCapContent(be, context));
        }

        @SuppressWarnings("unchecked")
        protected @Nullable T getCapContent(MBDMachine machine, C context) {
            List<T> contents = new ArrayList<>();
            for (var trait : machine.getAdditionalTraits()) {
                if (trait instanceof SimpleCapabilityTrait<?, ?> capabilityTrait && capabilityTrait.getDefinition().type() == this) {
                    var typedTrait = (SimpleCapabilityTrait<T, C>) capabilityTrait;
                    contents.add(typedTrait.getCapContent(typedTrait.getCapabilityIO(context)));
                }
            }
            return merge(contents);
        }

        /**
         * Collects capability content from the part's controllers, driven by the part's static
         * {@link ConfigPartSettings#proxyControllerCapabilities()} and any per-controller proxy capabilities
         * configured on matching {@code ProxyWhileFormed} predicates.
         */
        @SuppressWarnings("unchecked")
        protected @Nullable T getProxiedCapContent(MBDPartMachine part, C context) {
            var partSettings = part.getDefinition().partSettings();
            List<ConfigPartSettings.ProxyCapability> staticProxies = partSettings == null
                    ? List.of() : partSettings.proxyControllerCapabilities();
            var controllers = part.getControllers();
            if (controllers.isEmpty()) {
                return null;
            }
            Direction partFront = part.getFrontFacing().orElse(Direction.NORTH);
            Direction side = context instanceof Direction d ? d : null;
            List<T> contents = new ArrayList<>();
            for (var controller : controllers) {
                if (!(controller instanceof MBDMultiblockMachine mbdController)) continue;
                List<ConfigPartSettings.ProxyCapability> predicateProxies = part.getPredicateProxyCapabilities(mbdController.getPos());
                collectProxiedContents(mbdController, staticProxies, partFront, side, contents);
                collectProxiedContents(mbdController, predicateProxies, partFront, side, contents);
            }
            return merge(contents);
        }

        @SuppressWarnings("unchecked")
        protected @Nullable T getProxyPartCapContent(ProxyPartBlockEntity be, C context) {
            var caps = be.getProxyCapabilities();
            if (caps.isEmpty() || be.getLevel() == null || be.getControllerPos() == null) {
                return null;
            }
            var machine = IMachine.ofMachine(be.getLevel(), be.getControllerPos()).orElse(null);
            if (!(machine instanceof MBDMachine controller)) {
                return null;
            }
            Direction front = controller.getFrontFacing().orElse(Direction.NORTH);
            Direction side = context instanceof Direction d ? d : null;
            List<T> contents = new ArrayList<>();
            collectProxiedContents(controller, caps, front, side, contents);
            return merge(contents);
        }

        @SuppressWarnings("unchecked")
        private void collectProxiedContents(MBDMachine controller,
                                            List<ConfigPartSettings.ProxyCapability> proxies,
                                            Direction front,
                                            @Nullable Direction side,
                                            List<T> out) {
            if (proxies.isEmpty()) return;
            for (var proxy : proxies) {
                IO io = proxy.capabilityIO().getIO(front, side);
                if (io == IO.NONE) continue;
                String filter = proxy.traitNameFilter();
                for (var trait : controller.getAdditionalTraits()) {
                    if (!(trait instanceof SimpleCapabilityTrait<?, ?> capabilityTrait)) continue;
                    if (capabilityTrait.getDefinition().type() != this) continue;
                    if (filter != null && !filter.isEmpty()
                            && !trait.getDefinition().getName().contains(filter)) continue;
                    var typedTrait = (SimpleCapabilityTrait<T, C>) capabilityTrait;
                    out.add(typedTrait.getCapContent(io));
                }
            }
        }

        private @Nullable T mergeOpt(@Nullable T a, @Nullable T b) {
            if (a == null) return b;
            if (b == null) return a;
            return merge(List.of(a, b));
        }

        protected abstract BlockCapability<T, C> getCapability();

        protected abstract @Nullable T merge(List<T> contents);
    }

    @Configurable(name = "config.definition.trait.capability_io", subConfigurable = true,
            tips = {"config.definition.trait.capability_io.tooltip.0", "config.definition.trait.capability_io.tooltip.1"})
    private final CapabilityIO capabilityIO = new CapabilityIO();

    @Configurable(name = "config.definition.trait.gui_io", tips = "config.definition.trait.gui_io.tooltip")
    private IO guiIO = IO.BOTH;

    @Override
    public abstract SimpleCapabilityTrait<T, C> createTrait(MBDMachine machine);

    @Nullable
    protected T getCapContent(MBDMachine machine, C context) {
        if (machine.getTraitByDefinition(this) instanceof SimpleCapabilityTrait<?, ?> trait) {
            var traitCast = (SimpleCapabilityTrait<T, C>) trait;
            return traitCast.getCapContent(traitCast.getCapabilityIO(context));
        }
        return null;
    }
}
