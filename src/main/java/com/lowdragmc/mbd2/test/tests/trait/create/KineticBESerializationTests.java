package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlockEntity;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Verifies the kinetic BE's persistence sits correctly across both halves: Create's KineticBlockEntity
 * write/read AND LDLib2's MultiManagedStorage path (via BlockEntityMixin). Bugs surface as state lost
 * after save+reload — either kinetic speed, the workingSpeed generator field, or the embedded MBD2
 * machine state (custom name, recipe progress).
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class KineticBESerializationTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.GENERATOR_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void working_speed_survives_save_load(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.GENERATOR_MACHINE_ID, POS);
        var machine = scenario.machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity");
            return;
        }
        kineticBE.workingSpeed = 128f;
        // Save the BE's NBT (Create's write + LDLib2's mixin path) and read it into a fresh BE.
        var registries = h.getLevel().registryAccess();
        var savedTag = kineticBE.saveWithFullMetadata(registries);
        var newBE = BlockEntity.loadStatic(kineticBE.getBlockPos(), kineticBE.getBlockState(), savedTag, registries);
        if (!(newBE instanceof MBDKineticMachineBlockEntity reloadedBE)) {
            h.fail("Reloaded BE is not MBDKineticMachineBlockEntity");
            return;
        }
        if (reloadedBE.workingSpeed != 128f) {
            h.fail("workingSpeed did not round-trip: 128 -> " + reloadedBE.workingSpeed);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void kinetic_speed_survives_save_load(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS);
        var machine = scenario.machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity");
            return;
        }
        kineticBE.setSpeed(64f);
        var registries = h.getLevel().registryAccess();
        var savedTag = kineticBE.saveWithFullMetadata(registries);
        var newBE = BlockEntity.loadStatic(kineticBE.getBlockPos(), kineticBE.getBlockState(), savedTag, registries);
        if (!(newBE instanceof MBDKineticMachineBlockEntity reloadedBE)) {
            h.fail("Reloaded BE is not MBDKineticMachineBlockEntity");
            return;
        }
        if (reloadedBE.getSpeed() != 64f) {
            h.fail("Create kinetic speed did not round-trip: 64 -> " + reloadedBE.getSpeed());
            return;
        }
        h.succeed();
    }

    /**
     * Custom name (an MBD2 machine field synced via MultiManagedStorage) round-trips through the
     * BlockEntityMixin save-additional path.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void machine_custom_name_survives_save_load(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS);
        var machine = scenario.machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity");
            return;
        }
        if (!(kineticBE.getMetaMachine() instanceof com.lowdragmc.mbd2.common.machine.MBDMachine mbdMachine)) {
            h.fail("metaMachine is not MBDMachine");
            return;
        }
        mbdMachine.setCustomName(net.minecraft.network.chat.Component.literal("TestKineticMachine"));
        var registries = h.getLevel().registryAccess();
        var savedTag = kineticBE.saveWithFullMetadata(registries);
        var newBE = BlockEntity.loadStatic(kineticBE.getBlockPos(), kineticBE.getBlockState(), savedTag, registries);
        if (!(newBE instanceof MBDKineticMachineBlockEntity reloadedBE)) {
            h.fail("Reloaded BE is not MBDKineticMachineBlockEntity");
            return;
        }
        var reloadedName = reloadedBE.getCustomName();
        if (reloadedName == null || !"TestKineticMachine".equals(reloadedName.getString())) {
            h.fail("MBD2 custom name did not round-trip via MultiManagedStorage: " + reloadedName);
            return;
        }
        h.succeed();
    }
}
