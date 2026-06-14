package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.AECapabilities;
import appeng.api.storage.MEStorage;
import appeng.core.definitions.AEBlocks;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@lombok.Getter
@lombok.Setter
public class MEPatternProviderTraitDefinition extends SimpleCapabilityTraitDefinition<MEStorage, @Nullable Direction> {
    @LDLRegister(name = "ae2_me_pattern_provider", registry = "mbd2:trait_definition_type", group = "trait", priority = -100, modID = "ae2")
    public static final SimpleCapabilityTraitDefinition.Type<MEStorage, @Nullable Direction, MEPatternProviderTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("ae2_me_pattern_provider", "trait") {
                @Override
                public MEPatternProviderTraitDefinition createDefinition() {
                    return new MEPatternProviderTraitDefinition();
                }

                @Override
                public void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
                    event.registerBlockEntity(
                            AECapabilities.ME_STORAGE,
                            definition.blockEntityType(),
                            (be, context) -> {
                                if (be instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                                    return MEAECapabilityHelper.getStorage(machine, context);
                                }
                                return null;
                            });
                    event.registerBlockEntity(
                            AECapabilities.GENERIC_INTERNAL_INV,
                            definition.blockEntityType(),
                            (be, context) -> {
                                if (be instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                                    return MEAECapabilityHelper.getGenericInternalInventory(machine, context);
                                }
                                return null;
                            });
                    event.registerBlockEntity(
                            AECapabilities.IN_WORLD_GRID_NODE_HOST,
                            definition.blockEntityType(),
                            (be, context) -> {
                                if (be instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                                    return MEAECapabilityHelper.getGridNodeHost(machine);
                                }
                                return null;
                            });
                }

                @Override
                protected BlockCapability<MEStorage, @Nullable Direction> getCapability() {
                    return AECapabilities.ME_STORAGE;
                }

                @Override
                protected @Nullable MEStorage merge(List<MEStorage> contents) {
                    return switch (contents.size()) {
                        case 0 -> null;
                        case 1 -> contents.getFirst();
                        default -> new MEMultiStorage(contents);
                    };
                }
            };

    @Configurable(name = "config.definition.trait.ae2_me_pattern_provider.slot_size")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int slotSize = 9;

    @Configurable(name = "config.definition.trait.ae2_me_pattern_provider.pattern_size")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int patternSize = 9;

    @Configurable(name = "config.definition.trait.ae2_me_pattern_provider.item_capacity")
    @ConfigNumber(range = {1, 64})
    private int itemCapacity = 64;

    @Configurable(name = "config.definition.trait.ae2_me_pattern_provider.fluid_capacity")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int fluidCapacity = 4000;

    @Override
    public MEPatternProviderTrait createTrait(MBDMachine machine) {
        return new MEPatternProviderTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(AEBlocks.PATTERN_PROVIDER.asItem());
    }

    @Override
    public boolean allowMultiple() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(TraitDefinition other) {
        return !(other instanceof MEInterfaceTraitDefinition) && super.isCompatibleWith(other);
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        container.getLayout().flexDirection(FlexDirection.ROW);
        var slot = new AEPatternProviderSlot();
        slot.setId(uiId());
        slot.setupTemplate(patternSize, slotSize);
        container.addChild(slot);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof MEPatternProviderTrait providerTrait) {
            ui.selectRegex("^%s$".formatted(uiId()), AEPatternProviderSlot.class)
                    .forEach(slot -> slot.bindPatternProvider(providerTrait, patternSize, slotSize, getGuiIO()));
        }
    }
}
