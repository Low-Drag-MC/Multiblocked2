//package com.lowdragmc.mbd2.integration.naturesaura.trait;
//
//import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
//import com.lowdragmc.mbd2.api.capability.recipe.IO;
//import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
//import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
//import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
//import com.lowdragmc.mbd2.common.machine.MBDMachine;
//import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTrait;
//import com.lowdragmc.mbd2.integration.naturesaura.NaturesAuraRecipeCapability;
//import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
//import net.minecraft.core.BlockPos;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//
//public class AuraHandlerTrait extends RecipeCapabilityTrait implements IRecipeHandlerTrait<Integer> {
//    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(AuraHandlerTrait.class);
//    @Override
//    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }
//
//    public AuraHandlerTrait(MBDMachine machine, AuraHandlerTraitDefinition definition) {
//        super(machine, definition);
//    }
//
//    @Override
//    public AuraHandlerTraitDefinition getDefinition() {
//        return (AuraHandlerTraitDefinition) super.getDefinition();
//    }
//
//    @Override
//    public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
//        if (!compatibleWith(io)) return left;
//        if (simulate) return null;
//        var world = getMachine().getLevel();
//        var pos = getMachine().getPos();
//        int sum = left.stream().reduce(0, Integer::sum);
//        if (io == IO.IN) {
//            var spot = IAuraChunk.getHighestSpot(world, pos, getDefinition().getRadius(), pos);
//            var drained = IAuraChunk.getAuraChunk(world, spot).drainAura(spot, sum);
//            sum -= drained;
//        } else if (io == IO.OUT) {
//            BlockPos spot = IAuraChunk.getLowestSpot(world, pos, getDefinition().getRadius(), pos);
//            var stored = IAuraChunk.getAuraChunk(world, spot).storeAura(spot, sum);
//            sum -= stored;
//        }
//        return sum > 0 ? List.of(sum) : null;
//    }
//
//    @Override
//    public RecipeCapability<Integer> getRecipeCapability() {
//        return NaturesAuraRecipeCapability.CAP;
//    }
//
//    @Override
//    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
//        return List.of(this);
//    }
//}
