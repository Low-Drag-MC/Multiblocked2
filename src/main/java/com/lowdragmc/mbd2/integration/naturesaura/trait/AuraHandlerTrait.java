package com.lowdragmc.mbd2.integration.naturesaura.trait;

import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.integration.naturesaura.NaturesAuraRecipeCapability;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AuraHandlerTrait extends RecipeCapabilityTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(AuraHandlerTrait.class);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    private final AuraRecipeHandler recipeHandler = new AuraRecipeHandler();

    public AuraHandlerTrait(MBDMachine machine, AuraHandlerTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public AuraHandlerTraitDefinition getDefinition() {
        return (AuraHandlerTraitDefinition) super.getDefinition();
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    public class AuraRecipeHandler extends RecipeHandlerTrait<Integer> {
        protected AuraRecipeHandler() {
            super(AuraHandlerTrait.this, NaturesAuraRecipeCapability.CAP);
        }

        @Override
        public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            if (simulate) return null;
            var level = getMachine().getLevel();
            if (level == null) return left;
            var pos = getMachine().getPos();
            int radius = getDefinition().getRadius();
            int sum = left.stream().reduce(0, Integer::sum);
            if (io == IO.IN) {
                BlockPos spot = IAuraChunk.getHighestSpot(level, pos, radius, pos);
                int drained = IAuraChunk.getAuraChunk(level, spot).drainAura(spot, sum);
                sum -= drained;
            } else if (io == IO.OUT) {
                BlockPos spot = IAuraChunk.getLowestSpot(level, pos, radius, pos);
                int stored = IAuraChunk.getAuraChunk(level, spot).storeAura(spot, sum);
                sum -= stored;
            }
            return sum > 0 ? List.of(sum) : null;
        }
    }
}
