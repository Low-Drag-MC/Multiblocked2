package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.AutoIO;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.integration.mekanism.MekanismChemicalRecipeCapability;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ChemicalTankCapabilityTrait extends SimpleCapabilityTrait<IChemicalHandler, @Nullable Direction> implements IAutoIOTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ChemicalTankCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final ChemicalStorage[] storages;
    private final ChemicalRecipeHandler recipeHandler = new ChemicalRecipeHandler();
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<IChemicalHandler, @Nullable Direction>>> nearbyCache = new HashMap<>();

    public ChemicalTankCapabilityTrait(MBDMachine machine, ChemicalTankCapabilityTraitDefinition definition) {
        super(machine, definition);
        storages = createStorages();
    }

    @Override
    public ChemicalTankCapabilityTraitDefinition getDefinition() {
        return (ChemicalTankCapabilityTraitDefinition) super.getDefinition();
    }

    protected ChemicalStorage[] createStorages() {
        var result = new ChemicalStorage[getDefinition().getTankSize()];
        Predicate<ChemicalStack> validator = getDefinition().getFilterSettings().isEnable()
                ? getDefinition().getFilterSettings()
                : s -> true;
        for (int i = 0; i < result.length; i++) {
            result[i] = new ChemicalStorage(getDefinition().getCapacity(), validator);
            result[i].setOnContentsChanged(this::notifyListeners);
        }
        return result;
    }

    @Override
    public IChemicalHandler getCapContent(IO capabilityIO) {
        return new ChemicalStorageWrapper(storages, capabilityIO, getDefinition().isAllowSameChemicals());
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public @Nullable AutoIO getAutoIO() {
        return getDefinition().getAutoIO().isEnable() ? getDefinition().getAutoIO() : null;
    }

    @NotNull
    public BlockCapabilityCache<IChemicalHandler, @Nullable Direction> getNearbyCache(ServerLevel serverLevel, BlockPos pos, Direction side) {
        return nearbyCache.computeIfAbsent(pos, p -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(
                        mekanism.common.capabilities.Capabilities.CHEMICAL.block(), serverLevel, pos, direction));
    }

    @Override
    public void handleAutoIO(BlockPos port, @NotNull Direction side, IO io) {
        if (!(getMachine().getLevel() instanceof ServerLevel serverLevel)) return;
        var neighbor = getNearbyCache(serverLevel, port, side).getCapability();
        if (neighbor == null) return;
        Predicate<ChemicalStack> filter = getDefinition().getFilterSettings().isEnable()
                ? getDefinition().getFilterSettings()
                : s -> true;
        if (io.support(IO.IN)) {
            for (int t = 0; t < neighbor.getChemicalTanks(); t++) {
                var stack = neighbor.getChemicalInTank(t);
                if (stack.isEmpty() || !filter.test(stack)) continue;
                var simulated = neighbor.extractChemical(t, Long.MAX_VALUE, Action.SIMULATE);
                if (simulated.isEmpty()) continue;
                var sim = insertInto(storages, simulated, Action.SIMULATE);
                long inserted = simulated.getAmount() - sim.getAmount();
                if (inserted <= 0) continue;
                var actuallyDrained = neighbor.extractChemical(t, inserted, Action.EXECUTE);
                if (!actuallyDrained.isEmpty()) insertInto(storages, actuallyDrained, Action.EXECUTE);
            }
        }
        if (io.support(IO.OUT)) {
            for (var storage : storages) {
                var stack = storage.getStack();
                if (stack.isEmpty()) continue;
                var simulated = storage.extract(Long.MAX_VALUE, Action.SIMULATE, mekanism.api.AutomationType.EXTERNAL);
                if (simulated.isEmpty()) continue;
                var remaining = neighbor.insertChemical(simulated, Action.SIMULATE);
                long accepted = simulated.getAmount() - remaining.getAmount();
                if (accepted <= 0) continue;
                var actuallyExtracted = storage.extract(accepted, Action.EXECUTE, mekanism.api.AutomationType.EXTERNAL);
                if (!actuallyExtracted.isEmpty()) neighbor.insertChemical(actuallyExtracted, Action.EXECUTE);
            }
        }
    }

    private static ChemicalStack insertInto(ChemicalStorage[] storages, ChemicalStack stack, Action action) {
        ChemicalStack remaining = stack.copy();
        for (var storage : storages) {
            if (remaining.isEmpty()) break;
            remaining = storage.insert(remaining, action, mekanism.api.AutomationType.EXTERNAL);
        }
        return remaining;
    }

    public class ChemicalRecipeHandler extends RecipeHandlerTrait<ChemicalStackIngredient> {
        protected ChemicalRecipeHandler() {
            super(ChemicalTankCapabilityTrait.this, MekanismChemicalRecipeCapability.CAP);
        }

        @Override
        public List<ChemicalStackIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<ChemicalStackIngredient> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var containers = simulate ? Arrays.stream(storages).map(ChemicalStorage::copy).toArray(ChemicalStorage[]::new) : storages;
            var result = new ArrayList<ChemicalStackIngredient>();
            if (io == IO.IN) {
                for (var ingredient : left) {
                    long need = ingredient.amount();
                    for (var container : containers) {
                        var stack = container.getStack();
                        if (stack.isEmpty() || !ingredient.testType(stack)) continue;
                        var extracted = container.extract(need, Action.EXECUTE, mekanism.api.AutomationType.EXTERNAL);
                        need -= extracted.getAmount();
                        if (need <= 0) break;
                    }
                    if (need > 0) {
                        result.add(need == ingredient.amount() ? ingredient
                                : new ChemicalStackIngredient(ingredient.ingredient(), need));
                    }
                }
            } else if (io == IO.OUT) {
                for (var ingredient : left) {
                    var holders = ingredient.ingredient().getChemicalHolders();
                    if (holders.isEmpty()) continue;
                    var output = new ChemicalStack(holders.getFirst(), ingredient.amount());
                    long remaining = ingredient.amount();
                    for (var container : containers) {
                        if (remaining <= 0) break;
                        var attempt = output.copyWithAmount(remaining);
                        var leftAfter = container.insert(attempt, Action.EXECUTE, mekanism.api.AutomationType.EXTERNAL);
                        remaining = leftAfter.isEmpty() ? 0 : leftAfter.getAmount();
                    }
                    if (remaining > 0) {
                        result.add(remaining == ingredient.amount() ? ingredient
                                : new ChemicalStackIngredient(ingredient.ingredient(), remaining));
                    }
                }
            }
            return result.isEmpty() ? null : result;
        }
    }
}
