package com.lowdragmc.mbd2.integration.arsnouveau.trait;

import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.integration.arsnouveau.ArsSourceRecipeCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A machine that spends the Source held in the jars around it, the way an Enchanting Apparatus does.
 *
 * <p>The counterpart to {@link SourceStorageCapabilityTrait}: no buffer of its own, no capability, just
 * {@code SourceUtil} over a radius. This is how every Ars Nouveau device except the relays works, so a
 * machine built with this trait drops into an existing source-jar farm with no wiring at all.</p>
 *
 * <h2>The scan cache, and why it is not optional</h2>
 * Recipe matching runs on {@code Util.backgroundExecutor()} (see {@code RecipeLogic.supplyAsyncSearchingTask}),
 * and {@code SourceUtil} walks a Manhattan ball of block entities — a thousand-odd {@code getBlockEntity}
 * calls. Doing that off the game thread is a data race with chunk loading, so the simulate half of
 * {@link ArsSourceRecipeHandler} never touches the world: it answers from {@link #availableSource} /
 * {@link #freeSpace}, which {@link #serverTick()} refreshes on the game thread once every
 * {@link #scanInterval} ticks.
 *
 * <p>That is also why this trait does not simply say "yes" while simulating the way
 * {@code AuraHandlerTrait} does. It could get away with it for a per-craft cost — a recipe that cannot
 * pay just fails to start and is retried — but {@code RecipeLogic.handleTickRecipe} runs the per-tick IO
 * whenever the <em>match</em> succeeded and ignores what the execution returns, so an optimistic
 * simulate would let a per-tick source cost run for free.</p>
 */
public class NearbySourceTrait extends RecipeCapabilityTrait {
    private final ArsSourceRecipeHandler recipeHandler = new ArsSourceRecipeHandler();

    /** How far this machine reaches for source, in blocks. Ars Nouveau's own devices use 5 to 10. */
    public final RuntimeValue<Integer> radius =
            runtimeValues.ofInt("radius", () -> getDefinition().getRadius());
    /** How often the surroundings are re-counted, in ticks. */
    public final RuntimeValue<Integer> scanInterval =
            runtimeValues.ofInt("scan_interval", () -> getDefinition().getScanInterval());
    /** Whether taking or giving source draws Ars Nouveau's flying source orb between the two blocks. */
    public final RuntimeValue<Boolean> particles =
            runtimeValues.ofBool("particles", () -> getDefinition().isParticles());

    /**
     * What the last scan saw. Volatile because the recipe search thread reads them while the game
     * thread writes them; an int is written atomically, and a reader that sees the previous scan's
     * number is no worse off than one that ran a tick earlier.
     */
    private volatile int availableSource;
    private volatile int freeSpace;
    private boolean scannedOnce;

    public NearbySourceTrait(MBDMachine machine, NearbySourceTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public NearbySourceTraitDefinition getDefinition() {
        return (NearbySourceTraitDefinition) super.getDefinition();
    }

    /**
     * The effective radius — every reader goes through here.
     * <p>
     * Not named {@code getRadius()}: that would be a bean property with the same name as the
     * {@link #radius} slot field, and KubeJS/Rhino would have to pick one — the same reason
     * {@code AuraHandlerTrait.radiusBlocks()} is spelled the way it is.
     */
    public int radiusBlocks() {
        return radius.get();
    }

    /** Source the last scan found within {@link #radiusBlocks()}, in jars and other providers. */
    public int getAvailableSource() {
        return availableSource;
    }

    /** Room the last scan found within {@link #radiusBlocks()} to put source into. */
    public int getFreeSpace() {
        return freeSpace;
    }

    @Override
    public void serverTick() {
        if (!(getMachine().getLevel() instanceof ServerLevel)) return;
        var interval = Math.max(1, scanInterval.get());
        if (scannedOnce && getMachine().getOffsetTimer() % interval != 0) return;
        scannedOnce = true;
        rescan();
    }

    /**
     * Re-count the surroundings. Game thread only.
     *
     * <p>{@link #availableSource} is counted whichever way the handler faces, because it is also what
     * the machine's UI shows — an output-only machine that skipped it would read "Nearby Source: 0" for
     * ever while standing in a room full of jars. {@link #freeSpace} answers a question only an output
     * handler asks, so a machine that only spends source does not walk the neighbourhood twice.</p>
     */
    public void rescan() {
        if (!(getMachine().getLevel() instanceof ServerLevel level)) return;
        var pos = getMachine().getPos();
        var radius = radiusBlocks();
        availableSource = sumSource(SourceUtil.canTakeSource(pos, level, radius));
        freeSpace = getHandlerIO().support(IO.OUT)
                ? sumFreeSpace(SourceUtil.canGiveSource(pos, level, radius)) : 0;
    }

    /**
     * Take {@code moved} off whichever side of the cache the transfer came out of.
     *
     * <p>Clamped at zero rather than trusted: the count is a snapshot, and a jar drained by something
     * else since the last scan means the machine really did move less than the cache thought it could.</p>
     */
    private void spend(IO io, int moved) {
        if (moved <= 0) return;
        if (io == IO.IN) {
            availableSource = Math.max(0, availableSource - moved);
        } else {
            freeSpace = Math.max(0, freeSpace - moved);
            // the source did not vanish, it went into the jars next door — and those are what
            // availableSource counts, UI included
            availableSource = (int) Math.min(Integer.MAX_VALUE, (long) availableSource + moved);
        }
    }

    private static int sumSource(List<ISpecialSourceProvider> providers) {
        long total = 0;
        for (var provider : providers) {
            total += Math.max(0, provider.getSource().getSource());
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    private static int sumFreeSpace(List<ISpecialSourceProvider> providers) {
        long total = 0;
        for (var provider : providers) {
            var tile = provider.getSource();
            total += Math.max(0, tile.getMaxSource() - tile.getSource());
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    public class ArsSourceRecipeHandler extends RecipeHandlerTrait<Integer> {
        protected ArsSourceRecipeHandler() {
            super(NearbySourceTrait.this, ArsSourceRecipeCapability.CAP);
        }

        @Override
        public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            int required = left.stream().reduce(0, Integer::sum);
            if (required <= 0) return null;
            if (simulate) {
                // cached numbers only — see the class javadoc on which thread this runs
                var affordable = io == IO.IN ? availableSource : freeSpace;
                return required > affordable ? List.of(required - affordable) : null;
            }
            if (!(getMachine().getLevel() instanceof ServerLevel level)) return left;
            var pos = getMachine().getPos();
            var radius = radiusBlocks();
            int leftOver = io == IO.IN ? take(level, pos, radius, required) : give(level, pos, radius, required);
            // Book the move against the cache rather than rescanning. A per-tick source cost runs this
            // every tick, and a scan walks every block entity in range — the thing the cache exists to
            // keep off the hot path in the first place. Subtracting what actually moved keeps the next
            // match honest until serverTick() re-counts for real.
            spend(io, required - leftOver);
            return leftOver > 0 ? List.of(leftOver) : null;
        }

        /**
         * All or nothing, because {@code SourceUtil.takeSourceMultiple} is: it walks the providers, and
         * puts everything back if the total was not enough. Returning the full amount as unsatisfied is
         * therefore the honest answer — nothing was taken.
         */
        private int take(ServerLevel level, BlockPos pos, int radius, int required) {
            var drained = particles.get()
                    ? SourceUtil.takeSourceMultipleWithParticles(pos, level, radius, required)
                    : SourceUtil.takeSourceMultiple(pos, level, radius, required);
            return drained == null ? required : 0;
        }

        /**
         * Unlike taking, giving is best-effort: there is no upstream helper for it, and a partially
         * filled neighbourhood is a normal state rather than a failed transaction.
         *
         * <p>The amount moved is measured rather than taken from the return value.
         * {@code ISourceTile#addSource(int, boolean)} is a <em>default</em> method delegating to
         * {@code addSource(int)}, which returns the resulting total, not the amount added — so a tile
         * that has not overridden it reports a number that would run this loop backwards. Reading the
         * stored amount either side of the call is the only answer that holds for every implementation.</p>
         */
        private int give(ServerLevel level, BlockPos pos, int radius, int required) {
            int remaining = required;
            var spawnParticles = particles.get();
            for (var provider : SourceUtil.canGiveSource(pos, level, radius)) {
                if (remaining <= 0) break;
                var tile = provider.getSource();
                var before = tile.getSource();
                var toAdd = Math.min(remaining, tile.getMaxSource() - before);
                if (toAdd <= 0) continue;
                tile.addSource(toAdd);
                var added = Math.max(0, tile.getSource() - before);
                if (added <= 0) continue;
                remaining -= added;
                if (spawnParticles) {
                    EntityFollowProjectile.spawn(level, pos, provider.getCurrentPos());
                }
            }
            return remaining;
        }
    }
}
