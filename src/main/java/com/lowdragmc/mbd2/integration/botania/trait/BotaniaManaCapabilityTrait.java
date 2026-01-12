package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.*;
import com.lowdragmc.mbd2.integration.botania.BotaniaManaRecipeCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.util.List;
import java.util.Optional;

public class BotaniaManaCapabilityTrait extends SimpleCapabilityTrait implements IAutoIOTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BotaniaManaCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableManaPool storage;
    private final ManaRecipeHandler recipeHandler = new ManaRecipeHandler();
    private final ManaReceiverCap manaReceiverCap = new ManaReceiverCap();
    private final SparkAttachableCap sparkAttachableCap = new SparkAttachableCap();

    public BotaniaManaCapabilityTrait(MBDMachine machine, BotaniaManaCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = createStorages(machine);
        storage.setOnContentsChanged(this::notifyListeners);
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
        return new CopiableManaPool(machine, getDefinition().getCapacity(), getDefinition().isCanAttachSpark());
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public List<ICapabilityProviderTrait<?>> getCapabilityProviderTraits() {
        return List.of(manaReceiverCap, sparkAttachableCap);
    }

    @Override
    public @Nullable AutoIO getAutoIO() {
        return getDefinition().getAutoIO().isEnable() ? getDefinition().getAutoIO() : null;
    }

    @Override
    public void handleAutoIO(BlockPos port, Direction side, IO io) {
        if (io.support(IO.IN)) {
            Optional.ofNullable(getMachine().getLevel().getBlockEntity(port.relative(side)))
                    .flatMap(be -> be.getCapability(BotaniaForgeCapabilities.MANA_RECEIVER, side.getOpposite()).resolve())
                    .ifPresent(source -> {
                        var available = source.getCurrentMana();
                        var cost = Math.min(available, storage.getMaxMana() - storage.getCurrentMana());
                        storage.receiveMana(cost);
                        source.receiveMana(-cost);
                    });
        }
        if (io.support(IO.OUT)) {
            Optional.ofNullable(getMachine().getLevel().getBlockEntity(port.relative(side)))
                    .flatMap(be -> be.getCapability(BotaniaForgeCapabilities.MANA_RECEIVER, side.getOpposite()).resolve())
                    .ifPresent(target -> {
                        var available = storage.getCurrentMana();
                        var cost = target instanceof ManaPool pool ? Math.min(available,  pool.getMaxMana() - pool.getCurrentMana()) : available;
                        storage.receiveMana(-cost);
                        target.receiveMana(cost);
                    });
        }
    }

    public class ManaRecipeHandler extends RecipeHandlerTrait<Integer> {
        protected ManaRecipeHandler() {
            super(BotaniaManaCapabilityTrait.this, BotaniaManaRecipeCapability.CAP);
        }

        @Override
        public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
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
    }

    public class ManaReceiverCap implements ICapabilityProviderTrait<ManaPool> {

        @Override
        public IO getCapabilityIO(@Nullable Direction side) {
            return BotaniaManaCapabilityTrait.this.getCapabilityIO(side);
        }

        @Override
        public Capability<ManaReceiver> getCapability() {
            return BotaniaForgeCapabilities.MANA_RECEIVER;
        }

        @Override
        public ManaPool getCapContent(IO capbilityIO) {
            return new ManaPoolWrapper(storage, capbilityIO);
        }

        @Override
        public ManaPool mergeContents(List<ManaPool> contents) {
            return new ManaPoolList(contents.toArray(new ManaPool[0]));
        }
    }

    public class SparkAttachableCap implements ICapabilityProviderTrait<SparkAttachable> {

        @Override
        public IO getCapabilityIO(@Nullable Direction side) {
            return BotaniaManaCapabilityTrait.this.getCapabilityIO(side);
        }

        @Override
        public Capability<SparkAttachable> getCapability() {
            return BotaniaForgeCapabilities.SPARK_ATTACHABLE;
        }

        @Override
        public SparkAttachable getCapContent(IO capbilityIO) {
            return storage;
        }

        @Override
        public SparkAttachable mergeContents(List<SparkAttachable> contents) {
            return contents.get(0);
        }

    }
}
