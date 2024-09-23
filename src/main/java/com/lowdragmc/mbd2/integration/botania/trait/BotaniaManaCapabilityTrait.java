package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.mana.ManaPool;

import java.util.List;

public class BotaniaManaCapabilityTrait extends SimpleCapabilityTrait<ManaPool, Integer> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BotaniaManaCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableManaPool storage;

    public BotaniaManaCapabilityTrait(MBDMachine machine, BotaniaManaCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = createStorages(machine);
    }

    @Override
    public BotaniaManaCapabilityTraitDefinition getDefinition() {
        return (BotaniaManaCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        storage.receiveMana(getDefinition().getCapacity() / 2);
    }

    protected CopiableManaPool createStorages(MBDMachine machine) {
        return new CopiableManaPool(machine.getLevel(), machine.getPos(), getDefinition().getCapacity());
    }

    @Override
    public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
        int required = left.stream().reduce(0, Integer::sum);
        var capability = simulate ? storage.copy() : storage;
        if (io == IO.IN) {
            var cost = Math.min(required, capability.getCurrentMana());
            capability.receiveMana(-cost);
            required -= cost;
        } else {
            if (capability.isFull() || !capability.canReceiveManaFromBursts()) return left;
            if (required > (capability.getMaxMana() - capability.getCurrentMana())) {
                var received = capability.getMaxMana() - capability.getCurrentMana();
                capability.receiveMana(received);
                required -= received;
            } else {
                capability.receiveMana(required);
                return null;
            }
        }
        return required > 0 ? List.of(required) : null;
    }

    @Override
    public ManaPool getCapContent(@Nullable Direction side) {
        return new ManaPoolWrapper(this.storage, getCapabilityIO(side));
    }

    @Override
    public ManaPool mergeContents(List<ManaPool> contents) {
        return new ManaPoolList(contents.toArray(new ManaPool[0]));
    }
}
