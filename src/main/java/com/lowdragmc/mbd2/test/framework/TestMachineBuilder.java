package com.lowdragmc.mbd2.test.framework;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.forgeenergy.ForgeEnergyCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fluent builder for test-only MBD machines. Wraps MBDMachineDefinition.builder()
 * / MultiblockMachineDefinition.builder() with sensible defaults and convenience
 * methods for attaching common traits.
 */
public class TestMachineBuilder {

    private final ResourceLocation id;
    private final boolean multiblock;
    private final List<TraitDefinition> traits = new ArrayList<>();
    private final List<Consumer<ConfigPartSettings>> partSettingsTweaks = new ArrayList<>();
    @Nullable private ResourceLocation recipeTypeId;
    @Nullable private Function<MBDMultiblockMachine, BlockPattern> blockPatternFactory;
    @Nullable private MachineState rootState;

    private TestMachineBuilder(ResourceLocation id, boolean multiblock) {
        this.id = id;
        this.multiblock = multiblock;
    }

    public static TestMachineBuilder simple(ResourceLocation id) {
        return new TestMachineBuilder(id, false);
    }

    public static TestMachineBuilder multiblock(ResourceLocation id) {
        return new TestMachineBuilder(id, true);
    }

    /** Attach an item-slot trait. {@code count} slots, default slot limit 64, IO=BOTH. */
    public TestMachineBuilder withItemSlots(int count) {
        return withItemSlots(count, IO.BOTH, def -> {});
    }

    /** Attach an item-slot trait with a specific recipe IO direction (IN for inputs, OUT for outputs). */
    public TestMachineBuilder withItemSlots(int count, IO io) {
        return withItemSlots(count, io, def -> {});
    }

    public TestMachineBuilder withItemSlots(int count, IO io, Consumer<ItemSlotCapabilityTraitDefinition> tweak) {
        var def = new ItemSlotCapabilityTraitDefinition();
        def.setSlotSize(count);
        def.setRecipeHandlerIO(io);
        // each item-slot trait needs a unique name so its persisted-storage prefix doesn't collide
        // with sibling item-slot traits (see MBDMachine.loadAdditionalTraits's "trait.<name>" prefix)
        long existing = traits.stream().filter(t -> t instanceof ItemSlotCapabilityTraitDefinition).count();
        if (existing > 0) def.setName("item_slot_" + existing);
        tweak.accept(def);
        traits.add(def);
        return this;
    }

    /** Attach a fluid-tank trait. */
    public TestMachineBuilder withFluidTanks(int tankCount, int capacity) {
        return withFluidTanks(tankCount, capacity, def -> {});
    }

    public TestMachineBuilder withFluidTanks(int tankCount, int capacity, Consumer<FluidTankCapabilityTraitDefinition> tweak) {
        var def = new FluidTankCapabilityTraitDefinition();
        def.setTankSize(tankCount);
        def.setCapacity(capacity);
        // unique name to avoid persisted-storage prefix collisions between sibling fluid_tank traits
        long existing = traits.stream().filter(t -> t instanceof FluidTankCapabilityTraitDefinition).count();
        if (existing > 0) def.setName("fluid_tank_" + existing);
        tweak.accept(def);
        traits.add(def);
        return this;
    }

    /** Attach an energy trait. */
    public TestMachineBuilder withEnergy(int capacity) {
        return withEnergy(capacity, def -> {});
    }

    public TestMachineBuilder withEnergy(int capacity, Consumer<ForgeEnergyCapabilityTraitDefinition> tweak) {
        var def = new ForgeEnergyCapabilityTraitDefinition();
        def.setCapacity(capacity);
        def.setMaxReceive(capacity);
        def.setMaxExtract(capacity);
        tweak.accept(def);
        traits.add(def);
        return this;
    }

    /** Attach an arbitrary pre-built trait. Escape hatch for traits we don't have a convenience for. */
    public TestMachineBuilder withTrait(TraitDefinition definition) {
        traits.add(definition);
        return this;
    }

    /**
     * Mutate the part's {@link ConfigPartSettings} when it's created via the part settings factory.
     * Only effective for non-multiblock (part) machines, since multiblock definitions don't allow part settings.
     */
    public TestMachineBuilder withPartSettings(Consumer<ConfigPartSettings> tweak) {
        if (multiblock) {
            throw new IllegalStateException("withPartSettings is only valid for simple(...) (part) machines");
        }
        partSettingsTweaks.add(tweak);
        return this;
    }

    /** Bind this machine to a recipe type (by id). */
    public TestMachineBuilder withRecipeType(ResourceLocation recipeTypeId) {
        this.recipeTypeId = recipeTypeId;
        return this;
    }

    public TestMachineBuilder withRecipeType(MBDRecipeType recipeType) {
        return withRecipeType(recipeType.getRegistryName());
    }

    public TestMachineBuilder withRootState(MachineState rootState) {
        this.rootState = rootState;
        return this;
    }

    /** For multiblock machines only — supplies the controller's BlockPattern. */
    public TestMachineBuilder withBlockPattern(Function<MBDMultiblockMachine, BlockPattern> factory) {
        if (!multiblock) {
            throw new IllegalStateException("withBlockPattern is only valid for multiblock(...) machines");
        }
        this.blockPatternFactory = factory;
        return this;
    }

    public TestMachineBuilder withBlockPattern(BlockPattern pattern) {
        return withBlockPattern(controller -> pattern);
    }

    /** Build, register on the event, and return the resulting definition. */
    public MBDMachineDefinition register(MBDRegistryEvent.Machine event) {
        MachineState rootState = this.rootState != null ? this.rootState :
                (multiblock ? StateMachine.createMultiblockDefault(MachineState::baseBuilder, () -> IRenderer.EMPTY) : StateMachine.createDefault(MachineState::baseBuilder));
        ConfigRecipeLogicSettings recipeLogic = ConfigRecipeLogicSettings.builder()
                .recipeType(recipeTypeId != null ? recipeTypeId : MBDRecipeType.DUMMY.getRegistryName())
                .build();
        var msFactory = (com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition.ConfigMachineSettingsFactory) () -> {
            ConfigMachineSettings settings = ConfigMachineSettings.builder().build();
            for (TraitDefinition trait : traits) {
                settings.addTraitDefinition(trait);
            }
            return settings;
        };

        MBDMachineDefinition definition;
        if (multiblock) {
            MultiblockMachineDefinition.Builder mbBuilder = MultiblockMachineDefinition.builder();
            mbBuilder.id(id)
                    .rootState(rootState)
                    .blockProperties(ConfigBlockProperties.builder().build())
                    .itemProperties(ConfigItemProperties.builder().build())
                    .machineSettings(msFactory)
                    .recipeLogicSettings(recipeLogic);
            MultiblockMachineDefinition mb = mbBuilder.build();
            if (blockPatternFactory != null) {
                mb.blockPatternFactory(blockPatternFactory);
            }
            definition = mb;
        } else {
            var simpleBuilder = MBDMachineDefinition.builder()
                    .id(id)
                    .rootState(rootState)
                    .blockProperties(ConfigBlockProperties.builder().build())
                    .itemProperties(ConfigItemProperties.builder().build())
                    .machineSettings(msFactory)
                    .recipeLogicSettings(recipeLogic);
            if (!partSettingsTweaks.isEmpty()) {
                simpleBuilder.partSettings(() -> {
                    var settings = ConfigPartSettings.builder().build();
                    for (var tweak : partSettingsTweaks) tweak.accept(settings);
                    return settings;
                });
            }
            definition = simpleBuilder.build();
        }
        event.register(definition);
        return definition;
    }
}
