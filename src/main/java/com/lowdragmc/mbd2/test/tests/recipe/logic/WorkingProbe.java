package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An item input handler that handles nothing — every recipe passes straight through it — and only counts the
 * {@code preWorking} / {@code postWorking} calls it gets, the way a handler with working state (a generator, a
 * lamp) would see them.
 *
 * <p>Attached behind the logic's real item input handlers, so matching never reaches it.</p>
 */
public class WorkingProbe implements IRecipeHandler<SizedIngredient> {
    public int pre;
    public int post;
    /** What each call was handed as its holder. */
    public final List<IRecipeCapabilityHolder> holders = new ArrayList<>();

    /** Attach a fresh probe to the item inputs {@code logic} uses. */
    public static WorkingProbe attach(RecipeLogic logic) {
        var probe = new WorkingProbe();
        var handlers = logic.getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP);
        if (handlers == null) {
            throw new IllegalStateException(logic + " has no item input handlers to sit behind");
        }
        handlers.add(probe);
        return probe;
    }

    /** Calls to {@code preWorking} not yet answered by a {@code postWorking}. */
    public int active() {
        return pre - post;
    }

    @Override
    public @Nullable List<SizedIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<SizedIngredient> left,
                                                             @Nullable String slotName, boolean simulate) {
        return left;
    }

    @Override
    public RecipeCapability<SizedIngredient> getRecipeCapability() {
        return ItemRecipeCapability.CAP;
    }

    @Override
    public void preWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
        if (io == IO.IN) {
            pre++;
            holders.add(holder);
        }
    }

    @Override
    public void postWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
        if (io == IO.IN) {
            post++;
            holders.add(holder);
        }
    }

    @Override
    public String toString() {
        return "WorkingProbe{pre=" + pre + ", post=" + post + "}";
    }
}
