package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * A trait that have recipe handling capability.
 */
public abstract class RecipeCapabilityTrait extends Trait {
    // per-machine overrides of the values authored on the definition
    /**
     * Which way this trait's handler faces in a recipe — the machine-side half of "is this an input or an
     * output".
     * <p>
     * Carries an {@code onChanged} hook because, unlike {@link #distinct} and {@link #slotNames}, this
     * value is not only read from the recipe engine's hot path: it is also the key
     * {@link MBDMachine#initCapabilitiesProxy()} buckets every handler under, once, at trait load. An
     * override applied afterwards would leave the handler in the old bucket and be ignored.
     */
    public final RuntimeValue<IO> recipeHandlerIO =
            runtimeValues.ofEnum("recipe_handler_io", IO.class, () -> getDefinition().getRecipeHandlerIO())
                    .onChanged(this::rebuildCapabilitiesProxy);
    /** Whether this handler is matched on its own rather than pooled with its siblings. */
    public final RuntimeValue<Boolean> distinct =
            runtimeValues.ofBool("distinct", () -> getDefinition().isDistinct());
    /**
     * The named slots this handler answers for. Same {@code onChanged} reasoning as
     * {@link #recipeHandlerIO}: a formed controller wraps its parts' handlers in a
     * {@code RecipeHandlerSlotsProxy} built from this list at proxy-init time.
     */
    public final RuntimeValue<List<String>> slotNames =
            runtimeValues.ofStringList("slot_names", () -> getDefinition().getSlotNames())
                    .onChanged(this::rebuildCapabilitiesProxy);
    /**
     * {@link #getSlotNames()}'s memo: the list it was built from, and the set built from it.
     * <p>
     * Volatile and replaced rather than mutated — recipe matching runs on a background search thread as
     * well as the game thread, so a reader must never see a half-built pair.
     */
    @Nullable
    private volatile Tuple<List<String>, Set<String>> slotNamesView;

    public RecipeCapabilityTrait(MBDMachine machine, RecipeCapabilityTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public RecipeCapabilityTraitDefinition getDefinition() {
        return (RecipeCapabilityTraitDefinition) super.getDefinition();
    }

    public IO getHandlerIO() {
        return recipeHandlerIO.get();
    }

    public boolean isDistinct() {
        return distinct.get();
    }

    /**
     * The named slots this handler answers for, as the recipe engine wants them.
     * <p>
     * On {@link com.lowdragmc.mbd2.api.recipe.MBDRecipe}'s hot path — called for every handler on every
     * match attempt — so it is worth not allocating. The empty case is by far the most common (a
     * definition names no slots unless the author says so) and now costs nothing at all, where the
     * previous {@code new HashSet<>(...)} allocated even for an empty list. The rest is memoised against
     * the list it was built from, so an override or an editor edit still shows up on the next read.
     */
    public Set<String> getSlotNames() {
        var names = slotNames.get();
        if (names.isEmpty()) return Set.of();
        var cached = slotNamesView;
        if (cached != null && cached.getA().equals(names)) return cached.getB();
        var view = Set.copyOf(names);
        slotNamesView = new Tuple<>(names, view);
        return view;
    }

    /**
     * Re-bucket this machine's recipe handlers, and those of every controller this machine is a part of.
     * <p>
     * The controller pass is the one that is easy to forget: a part's handlers are copied into the
     * controller's proxy table by {@code MBDMultiblockMachine.initCapabilitiesProxy}, so overriding a
     * part's handler IO without it would change nothing the controller's recipe logic can see.
     */
    private void rebuildCapabilitiesProxy() {
        var machine = getMachine();
        if (machine == null) return;
        machine.initCapabilitiesProxy();
        // getControllers() resolves block entities, which needs a level — and a hook can run during a
        // chunk load or an editor preview, where there is none yet. The controller rebuilds its own proxy
        // when it forms, so skipping here loses nothing.
        if (machine instanceof IMultiPart part && machine.getLevel() != null) {
            for (var controller : part.getControllers()) {
                if (controller instanceof MBDMachine controllerMachine) {
                    controllerMachine.initCapabilitiesProxy();
                }
            }
        }
    }
}
