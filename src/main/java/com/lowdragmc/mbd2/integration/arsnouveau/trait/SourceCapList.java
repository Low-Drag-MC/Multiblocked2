package com.lowdragmc.mbd2.integration.arsnouveau.trait;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;

import java.util.Arrays;

/**
 * One {@code ars_nouveau:source} capability standing in for several — what a machine carrying more than
 * one source trait, or a part proxying its controllers', hands out.
 *
 * @see com.lowdragmc.mbd2.common.trait.forgeenergy.EnergyStorageList
 */
public record SourceCapList(ISourceCap[] storages) implements ISourceCap {

    @Override
    public int receiveSource(int source, boolean simulate) {
        int received = 0;
        for (var storage : storages) {
            received += storage.receiveSource(source - received, simulate);
            if (received >= source) break;
        }
        return received;
    }

    @Override
    public int extractSource(int source, boolean simulate) {
        int extracted = 0;
        for (var storage : storages) {
            extracted += storage.extractSource(source - extracted, simulate);
            if (extracted >= source) break;
        }
        return extracted;
    }

    @Override
    public int getMaxExtract() {
        return Arrays.stream(storages).mapToInt(ISourceCap::getMaxExtract).sum();
    }

    @Override
    public int getMaxReceive() {
        return Arrays.stream(storages).mapToInt(ISourceCap::getMaxReceive).sum();
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
        return Arrays.stream(storages).anyMatch(ISourceCap::canReceive);
    }

    @Override
    public boolean canExtract() {
        return Arrays.stream(storages).anyMatch(ISourceCap::canExtract);
    }

    @Override
    public int getSource() {
        return Arrays.stream(storages).mapToInt(ISourceCap::getSource).sum();
    }

    @Override
    public int getSourceCapacity() {
        return Arrays.stream(storages).mapToInt(ISourceCap::getSourceCapacity).sum();
    }

    /** @see SourceCapWrapper#setSource(int) — inert for the same reason, and each member is anyway. */
    @Override
    public void setSource(int source) {}

    @Override
    public void setMaxSource(int max) {}
}
