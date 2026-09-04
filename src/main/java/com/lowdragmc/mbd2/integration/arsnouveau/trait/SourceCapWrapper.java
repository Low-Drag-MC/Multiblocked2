package com.lowdragmc.mbd2.integration.arsnouveau.trait;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.lowdragmc.mbd2.api.capability.recipe.IO;

/**
 * What a neighbour sees when it asks the machine for {@code ars_nouveau:source} on a given side:
 * the trait's storage, restricted to what that side is configured to allow and to the machine's
 * per-operation transfer rates.
 *
 * <p>The Ars Nouveau counterpart of
 * {@link com.lowdragmc.mbd2.common.trait.forgeenergy.EnergyStorageWrapper}.</p>
 */
public record SourceCapWrapper(CopiableSourceStorage storage,
                               IO io,
                               int maxReceive,
                               int maxExtract) implements ISourceCap {

    @Override
    public int receiveSource(int source, boolean simulate) {
        if (io == IO.IN || io == IO.BOTH) {
            return storage.receiveSource(Math.min(this.maxReceive, source), simulate);
        }
        return 0;
    }

    @Override
    public int extractSource(int source, boolean simulate) {
        if (io == IO.OUT || io == IO.BOTH) {
            return storage.extractSource(Math.min(this.maxExtract, source), simulate);
        }
        return 0;
    }

    /**
     * An Arcane Relay sizes its transfer with this before it simulates, so a side that cannot give has
     * to report zero here as well — otherwise the relay picks an amount, simulates it, gets nothing
     * back, and keeps trying every second forever.
     */
    @Override
    public int getMaxExtract() {
        return io == IO.OUT || io == IO.BOTH ? Math.min(maxExtract, storage.getMaxExtract()) : 0;
    }

    @Override
    public int getMaxReceive() {
        return io == IO.IN || io == IO.BOTH ? Math.min(maxReceive, storage.getMaxReceive()) : 0;
    }

    @Override
    public boolean canAcceptSource(int source) {
        return receiveSource(source, true) > 0;
    }

    @Override
    public boolean canProvideSource(int source) {
        return extractSource(source, true) > 0;
    }

    @Override
    public boolean canReceive() {
        return getMaxReceive() > 0;
    }

    @Override
    public boolean canExtract() {
        return getMaxExtract() > 0;
    }

    @Override
    public int getSource() {
        return storage.getSource();
    }

    @Override
    public int getSourceCapacity() {
        return storage.getSourceCapacity();
    }

    /**
     * Deliberately inert, both of them.
     * <p>
     * These two are {@code ISourceCap}'s escape hatches — "force set the amount, ignoring transfer
     * rates" and "resize the buffer". Neither is something a neighbour gets to do to a machine: the
     * amount is what {@code receiveSource}/{@code extractSource} and the machine's own recipes decide,
     * and the capacity belongs to the trait's {@code capacity} runtime value, which would immediately
     * disagree with anything written here. Nothing in Ars Nouveau calls either through the capability —
     * the relays, its only consumers, use the transfer pair above.
     */
    @Override
    public void setSource(int source) {}

    @Override
    public void setMaxSource(int max) {}
}
