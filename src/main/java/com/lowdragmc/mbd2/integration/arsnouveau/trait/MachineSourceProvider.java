package com.lowdragmc.mbd2.integration.arsnouveau.trait;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import net.minecraft.core.BlockPos;

/**
 * The machine, seen by Ars Nouveau's devices as if it were a Source Jar.
 *
 * <h2>Why this exists at all</h2>
 * There are two disjoint ways to be a source machine in Ars Nouveau, and exposing
 * {@code ars_nouveau:source} only covers one of them. The capability is read by exactly two blocks —
 * the Arcane Relay and the Relay Splitter. Everything else (Enchanting Apparatus, Ritual Brazier,
 * Imbuement Chamber, Spell Turret, Drygmy, Whirlisprig, Wixie Cauldron, Sourcelinks, …) goes through
 * {@code SourceUtil}, which finds a {@code SourceJarTile} block entity or something registered in
 * {@link SourceManager} — and nothing else. {@link SourceManager#addInterface} is never called inside
 * Ars Nouveau itself; it is there for addons, and this is MBD2 taking it up.
 *
 * <h2>Two classes, because the interfaces collide</h2>
 * {@code ISpecialSourceProvider#getSource()} returns an {@code ISourceTile} and
 * {@code ISourceTile#getSource()} returns an {@code int}, so no single class can be both. Ars Nouveau's
 * own {@code SourceProvider} splits them the same way — but its {@code isValid()} is fixed to
 * {@code tile != null} at construction, i.e. permanently true, which would leave a broken machine in
 * {@link SourceManager}'s set forever. Hence our own provider with a real liveness answer.
 *
 * <h2>Transfer rates do not apply here</h2>
 * The trait's {@code maxReceive}/{@code maxExtract} govern <em>piping</em> — what a relay or auto-IO
 * may move per operation. To a device standing next to the machine, the machine is a jar, and its whole
 * buffer is available; that is what {@code SourceJarTile} does (its rates equal its capacity) and what
 * every caller of {@code SourceUtil} assumes. It also has to be that way for correctness:
 * {@code SourceUtil.takeSourceMultiple} rolls a failed multi-jar draw back with {@code addSource(n)} and
 * ignores the return value, so a capped {@code addSource} would quietly destroy source.
 */
public class MachineSourceProvider implements ISpecialSourceProvider {
    private final SourceStorageCapabilityTrait trait;
    private final Tile tile = new Tile();
    /**
     * Flipped by the trait's lifecycle hooks. {@link SourceManager} has no removal method — it prunes
     * providers that report themselves invalid, on every 60th tick — so going invalid is the only way out of
     * its set.
     */
    private boolean unloaded;

    public MachineSourceProvider(SourceStorageCapabilityTrait trait) {
        this.trait = trait;
    }

    /** Called from the trait when the machine loads, and when the toggle is turned back on. */
    public void onLoad() {
        unloaded = false;
    }

    public void onUnload() {
        unloaded = true;
    }

    @Override
    public ISourceTile getSource() {
        return tile;
    }

    @Override
    public boolean isValid() {
        if (unloaded || !trait.exposedToDevices()) return false;
        var machine = trait.getMachine();
        var holder = machine.getHolder();
        // a persistence round trip, a chunk reload and a broken block all end here: the old block entity
        // is marked removed and a new trait registers a new provider of its own
        return holder != null && !holder.isRemoved() && machine.getLevel() != null;
    }

    @Override
    public BlockPos getCurrentPos() {
        return trait.getMachine().getPos();
    }

    /**
     * The storage, in the shape {@code SourceUtil} expects.
     *
     * <p>The return values are the awkward part of {@code ISourceTile} and are not ours to redesign:
     * {@code addSource(int)} / {@code removeSource(int)} / {@code setSource(int)} return the
     * <b>resulting total</b>, while the {@code simulate} overloads return the <b>amount moved</b>.
     * {@code SourceUtil.takeSourceMultiple} reads an extraction as {@code before - after}, so getting
     * this backwards would look like the machine had infinite source. Matches
     * {@code AbstractSourceMachine} exactly.</p>
     */
    public class Tile implements ISourceTile {

        private CopiableSourceStorage storage() {
            return trait.getStorage();
        }

        @Override
        public int getTransferRate() {
            return trait.maxExtract.get();
        }

        @Override
        public boolean canAcceptSource() {
            return getSource() < getMaxSource();
        }

        @Override
        public boolean canProvideSource() {
            return getSource() > 0;
        }

        @Override
        public int getSource() {
            return storage().getSource();
        }

        @Override
        public int getMaxSource() {
            return storage().getSourceCapacity();
        }

        @Override
        public int setSource(int source) {
            storage().setSource(source);
            return getSource();
        }

        @Override
        public int addSource(int source) {
            return setSource(getSource() + source);
        }

        @Override
        public int addSource(int source, boolean simulate) {
            int added = Math.min(source, getMaxSource() - getSource());
            if (added <= 0) return 0;
            if (!simulate) setSource(getSource() + added);
            return added;
        }

        @Override
        public int removeSource(int source) {
            return setSource(getSource() - source);
        }

        @Override
        public int removeSource(int source, boolean simulate) {
            int removed = Math.min(source, getSource());
            if (removed <= 0) return 0;
            if (!simulate) setSource(getSource() - removed);
            return removed;
        }
    }
}
