package com.lowdragmc.mbd2.test.tests.bench;

import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;

/**
 * A machine that ticks only while {@link RecipeLogicBench} drives it, so the level's own ticks between the bench's
 * slices neither run it a second time nor at the wrong clock.
 */
public class BenchMachine extends MBDPartMachine {
    /** Set by the bench, on the server thread, around the ticks it simulates. */
    static boolean driving;

    public BenchMachine(IMachineBlockEntity holder, MBDMachineDefinition definition) {
        super(holder, definition);
    }

    @Override
    public void serverTick() {
        if (driving) {
            super.serverTick();
        }
    }
}
