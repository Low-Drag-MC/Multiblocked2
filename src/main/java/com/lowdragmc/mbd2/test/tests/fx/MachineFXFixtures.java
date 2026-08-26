package com.lowdragmc.mbd2.test.tests.fx;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * A machine whose {@code working} state carries a Photon effect.
 *
 * <p>Registered unconditionally, Photon or not: the effect configuration is plain MBD2 data, and a
 * definition that silently failed to load without Photon is exactly the regression
 * {@link MachineFXConfigTests} exists to catch. Only the playback is gated, and that happens far
 * downstream in {@code MachineFXManagers.create}.</p>
 */
public class MachineFXFixtures implements TestFixtureProvider {

    /** A machine with a per-state effect on {@code working} and nothing on the root state. */
    public static final ResourceLocation STATE_FX_MACHINE_ID = MBD2.id("test_state_fx_machine");

    /** The effect entry's name; the state identifier is this prefixed by {@code state:}. */
    public static final String FX_NAME = "working_fx";
    /** {@code assets/mbd2/fx/test_machine_fx.fx} — a single looping particle emitter. */
    public static final ResourceLocation TEST_FX = MBD2.id("test_machine_fx");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var definition = TestMachineBuilder.simple(STATE_FX_MACHINE_ID).register(event);
        // The default state machine is base -> working -> waiting, so hang the effect off working:
        // the test then watches it start and stop by moving the machine between base and working.
        var working = definition.stateMachine().getState("working");
        if (working == definition.stateMachine().getRootState()) {
            throw new IllegalStateException("the default state machine no longer has a 'working' state");
        }
        working.machineFXs().setEnable(true);
        working.machineFXs().getFxs().add(stateFX());
    }

    private static MachineFXConfig stateFX() {
        var fx = new MachineFXConfig(FX_NAME, TEST_FX);
        // above the block, so the particles are not buried inside the machine's own model
        fx.setOffset(new org.joml.Vector3f(0, 1, 0));
        return fx;
    }
}
