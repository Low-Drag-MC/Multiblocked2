package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public final class MEAECapabilityHelper {
    private MEAECapabilityHelper() {
    }

    public static @Nullable MEInterfaceTrait findInterfaceTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof MEInterfaceTrait interfaceTrait) {
                return interfaceTrait;
            }
        }
        return null;
    }

    public static @Nullable MEPatternProviderTrait findPatternProviderTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof MEPatternProviderTrait providerTrait) {
                return providerTrait;
            }
        }
        return null;
    }

    public static @Nullable MEStorage getStorage(MBDMachine machine, @Nullable Direction side) {
        var storages = new ArrayList<MEStorage>();
        var interfaceTrait = findInterfaceTrait(machine);
        if (interfaceTrait != null) {
            var storage = interfaceTrait.getCapContent(interfaceTrait.getCapabilityIO(side));
            if (storage != null) {
                storages.add(storage);
            }
        }
        var providerTrait = findPatternProviderTrait(machine);
        if (providerTrait != null) {
            var storage = providerTrait.getCapContent(providerTrait.getCapabilityIO(side));
            if (storage != null) {
                storages.add(storage);
            }
        }
        return switch (storages.size()) {
            case 0 -> null;
            case 1 -> storages.getFirst();
            default -> new MEMultiStorage(storages);
        };
    }

    public static @Nullable GenericInternalInventory getGenericInternalInventory(MBDMachine machine, @Nullable Direction side) {
        var interfaceTrait = findInterfaceTrait(machine);
        if (interfaceTrait != null) {
            var inventory = interfaceTrait.getGenericInternalInventory(interfaceTrait.getCapabilityIO(side));
            if (inventory != null) {
                return inventory;
            }
        }
        var providerTrait = findPatternProviderTrait(machine);
        if (providerTrait != null) {
            return providerTrait.getGenericInternalInventory(providerTrait.getCapabilityIO(side));
        }
        return null;
    }

    public static @Nullable IInWorldGridNodeHost getGridNodeHost(MBDMachine machine) {
        var interfaceTrait = findInterfaceTrait(machine);
        if (interfaceTrait != null) {
            return interfaceTrait;
        }
        return findPatternProviderTrait(machine);
    }
}
