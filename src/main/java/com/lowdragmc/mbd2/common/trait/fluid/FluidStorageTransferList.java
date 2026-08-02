package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.misc.FluidTransferList;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

/**
 * A {@link FluidTransferList} of the machine tanks which respects the allowSameFluids setting while filling.
 * <br>
 * The vanilla {@link FluidTransferList} always spreads the given resource over all tanks, which ignores
 * {@link FluidTankCapabilityTraitDefinition#isAllowSameFluids()} while the auto io is filling the tanks.
 */
public class FluidStorageTransferList extends FluidTransferList {

    private final FluidStorage[] storages;
    private final boolean allowSameFluids;

    public FluidStorageTransferList(FluidStorage[] storages, boolean allowSameFluids) {
        super(storages);
        this.storages = storages;
        this.allowSameFluids = allowSameFluids;
    }

    @Override
    public long fill(FluidStack resource, boolean simulate, boolean notifyChanges) {
        if (allowSameFluids) {
            return super.fill(resource, simulate, notifyChanges);
        }
        return FluidHandlerWrapper.fillStorages(storages, false, resource, simulate, notifyChanges);
    }
}
