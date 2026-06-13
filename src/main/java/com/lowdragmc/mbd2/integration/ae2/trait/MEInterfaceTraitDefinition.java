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
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Setter
public class MEInterfaceTraitDefinition extends SimpleCapabilityTraitDefinition<MEStorage, @Nullable Direction> {
    @LDLRegister(name = "ae2_me_interface", registry = "mbd2:trait_definition_type", group = "trait", priority = -100, modID = "ae2")
    public static final SimpleCapabilityTraitDefinition.Type<MEStorage, @Nullable Direction, MEInterfaceTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("ae2_me_interface", "trait") {
                @Override
                public MEInterfaceTraitDefinition createDefinition() {
                    return new MEInterfaceTraitDefinition();
                }

                @Override
                public void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
                    super.registerCapabilities(definition, event);
                    event.registerBlockEntity(
                            AECapabilities.GENERIC_INTERNAL_INV,
                            definition.blockEntityType(),
                            (be, context) -> {
                                var trait = findTrait(be);
                                return trait == null ? null : trait.getGenericInternalInventory(trait.getCapabilityIO(context));
                            });
                    event.registerBlockEntity(
                            AECapabilities.IN_WORLD_GRID_NODE_HOST,
                            definition.blockEntityType(),
                            (be, context) -> findTrait(be));
                }

                @Override
                protected BlockCapability<MEStorage, @Nullable Direction> getCapability() {
                    return AECapabilities.ME_STORAGE;
                }

                @Override
                protected @Nullable MEStorage merge(List<MEStorage> contents) {
                    return contents.isEmpty() ? null : contents.get(0);
                }

                private @Nullable MEInterfaceTrait findTrait(Object be) {
                    if (be instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                        for (var trait : machine.getAdditionalTraits()) {
                            if (trait instanceof MEInterfaceTrait interfaceTrait && interfaceTrait.getDefinition().type() == this) {
                                return interfaceTrait;
                            }
                        }
                    }
                    return null;
                }
            };

    @Configurable(name = "config.definition.trait.ae2_me_interface.slot_size")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int slotSize = 9;

    @Override
    public MEInterfaceTrait createTrait(MBDMachine machine) {
        return new MEInterfaceTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(AEBlocks.INTERFACE.asItem());
    }

    @Override
    public boolean allowMultiple() {
        return false;
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        container.getLayout().flexDirection(FlexDirection.ROW);
        for (var i = 0; i < this.slotSize; i++) {
            var slotWidget = new AEInterfaceSlot();
            slotWidget.setId(uiId() + "_" + i);
            container.addChild(slotWidget);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof MEInterfaceTrait interfaceTrait) {
            var prefix = uiId();
            var guiIO = getGuiIO();
            ui.selectRegex("^%s_[0-9]+$".formatted(prefix), AEInterfaceSlot.class).forEach(slotWidget -> {
                var idStr = slotWidget.getId();
                var lastUnderscore = idStr.lastIndexOf('_');
                if (lastUnderscore < 0) return;
                int index;
                try {
                    index = Integer.parseInt(idStr.substring(lastUnderscore + 1));
                } catch (NumberFormatException ignored) {
                    return;
                }
                if (index >= 0 && index < slotSize) {
                    slotWidget.setItemInterfaceLogic(interfaceTrait.getInterfaceLogic(), index);
                    slotWidget.setIngredientIO(guiIO);
                    slotWidget.setCanTakeItems(guiIO.support(IO.OUT));
                    slotWidget.setCanPutItems(guiIO.support(IO.IN));
                }
            });
        }
    }
}
