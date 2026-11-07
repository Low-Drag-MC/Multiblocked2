package com.lowdragmc.mbd2.api.recipe;

import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A recipe type's recipes in the order a search tries them, with an index of what their inputs need.
 *
 * <p>Each input content a capability can {@link RecipeCapability#getLookupKeys key} — a plain item or tag ingredient,
 * a single fluid or fluid tag — must be satisfied by one of its keys being held. {@link #candidates} drops every
 * recipe with such a content no input holds a key for, and only those: it is a necessary condition, never a
 * replacement for matching. A content that cannot be keyed is simply not used to drop anything.</p>
 *
 * <p>Immutable once built, so readable from any thread; rebuilt by {@link MBDRecipeType#getLookup} after recipes or
 * tags reload.</p>
 */
public final class RecipeLookup {
    /** At most this many keyed contents of one recipe are used to drop it; any more are just not checked. */
    private static final int MAX_KEYED = 64;

    private final RecipeManager manager;
    private final int generation;
    private final List<RecipeHolder<MBDRecipe>> sorted;
    /** Per recipe, one bit per keyed input content; all must be satisfied. Zero for a recipe with none. */
    private final long[] required;
    /** Per capability, per recipe: the bits of that recipe's contents of that capability. */
    private final Map<RecipeCapability<?>, long[]> capabilityBits;
    /** Per capability, per key: postings {@code recipe << 6 | bit}. */
    private final Map<RecipeCapability<?>, Map<Object, int[]>> postings;

    private RecipeLookup(RecipeManager manager, int generation, List<RecipeHolder<MBDRecipe>> sorted, long[] required,
                         Map<RecipeCapability<?>, long[]> capabilityBits, Map<RecipeCapability<?>, Map<Object, int[]>> postings) {
        this.manager = manager;
        this.generation = generation;
        this.sorted = sorted;
        this.required = required;
        this.capabilityBits = capabilityBits;
        this.postings = postings;
    }

    /** Sort {@code type}'s recipes by priority — stably, as the search always has — and index their inputs. */
    static RecipeLookup build(MBDRecipeType type, RecipeManager manager, int generation) {
        var recipes = new ArrayList<>(manager.getAllRecipesFor(type));
        recipes.sort(Comparator.comparingInt(recipe -> recipe.value().priority));
        int n = recipes.size();
        var required = new long[n];
        var capabilityBits = new IdentityHashMap<RecipeCapability<?>, long[]>();
        var building = new IdentityHashMap<RecipeCapability<?>, Map<Object, IntArrayList>>();
        for (int r = 0; r < n; r++) {
            int bit = 0;
            for (var entry : recipes.get(r).value().inputs.entrySet()) {
                var capability = entry.getKey();
                for (var content : entry.getValue()) {
                    if (bit >= MAX_KEYED) break;
                    var keys = capability.getLookupKeys(content.content);
                    if (keys == null) continue;
                    long mask = 1L << bit;
                    required[r] |= mask;
                    capabilityBits.computeIfAbsent(capability, c -> new long[n])[r] |= mask;
                    var byKey = building.computeIfAbsent(capability, c -> new HashMap<>());
                    for (var key : keys) {
                        byKey.computeIfAbsent(key, k -> new IntArrayList()).add(r << 6 | bit);
                    }
                    bit++;
                }
            }
        }
        var postings = new IdentityHashMap<RecipeCapability<?>, Map<Object, int[]>>();
        building.forEach((capability, byKey) -> {
            var frozen = new HashMap<Object, int[]>(byKey.size() * 2);
            byKey.forEach((key, list) -> frozen.put(key, list.toIntArray()));
            postings.put(capability, frozen);
        });
        return new RecipeLookup(manager, generation, List.copyOf(recipes), required, capabilityBits, postings);
    }

    boolean isFor(RecipeManager manager, int generation) {
        return this.manager == manager && this.generation == generation;
    }

    /** Every recipe of the type, in the order a search tries them. */
    public List<RecipeHolder<MBDRecipe>> getSorted() {
        return sorted;
    }

    /** The capabilities some recipe has a keyed input of: what {@link #candidates} wants to know the keys of. */
    public Set<RecipeCapability<?>> getIndexedCapabilities() {
        return capabilityBits.keySet();
    }

    /**
     * The recipes, in search order, whose keyed inputs can all be satisfied.
     *
     * @param available for each of {@link #getIndexedCapabilities()}, the keys the inputs hold — an empty collection
     *                  when there are no such inputs — or {@code null} when the inputs hold something unknown, in
     *                  which case no recipe is dropped on that capability's account. A capability missing from the
     *                  map counts as empty.
     */
    public List<RecipeHolder<MBDRecipe>> candidates(Map<RecipeCapability<?>, ? extends Collection<?>> available) {
        int n = sorted.size();
        var satisfied = new long[n];
        for (var entry : capabilityBits.entrySet()) {
            var capability = entry.getKey();
            if (available.containsKey(capability) && available.get(capability) == null) {
                var bits = entry.getValue();
                for (int r = 0; r < n; r++) {
                    satisfied[r] |= bits[r];
                }
                continue;
            }
            var keys = available.get(capability);
            if (keys == null || keys.isEmpty()) continue;
            var byKey = postings.get(capability);
            for (var key : keys) {
                var list = byKey.get(key);
                if (list == null) continue;
                for (int posting : list) {
                    satisfied[posting >>> 6] |= 1L << (posting & 63);
                }
            }
        }
        var result = new ArrayList<RecipeHolder<MBDRecipe>>();
        for (int r = 0; r < n; r++) {
            if ((satisfied[r] & required[r]) == required[r]) {
                result.add(sorted.get(r));
            }
        }
        return result;
    }
}
