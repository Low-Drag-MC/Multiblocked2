package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.mekanism.trait.chemical.ChemicalStorage;
import com.lowdragmc.mbd2.integration.mekanism.trait.chemical.ChemicalTankCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismChemicals;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class ChemicalTankTraitTests {
    static { @SuppressWarnings("unused") var ignored = ChemicalTankTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void chemical_capability_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ChemicalTankTraitFixtures.MACHINE_ID, POS)
                .assertExposes(Capabilities.CHEMICAL.block(), null)
                .succeed();
    }

    /**
     * The {@code capacity} runtime value. Unlike the fluid and energy storages this one is read live —
     * {@code BasicChemicalTank} routes every internal capacity read through {@code getCapacity()} — so
     * the only thing the change hook has to do is spill what no longer fits.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void chemical_capacity_override_resizes_and_spills(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ChemicalTankTraitFixtures.MACHINE_ID, POS)
                .check("the tank starts at its authored capacity",
                        m -> tank(m).getCapacity() == 16_000)
                .with(m -> tank(m).insertChemical(hydrogen(16_000), Action.EXECUTE))
                .check("and fills to it", m -> tank(m).getStored() == 16_000)
                .with(m -> chemicalTrait(m).capacity.set(32_000L))
                .check("growing the tank should be visible to a reader",
                        m -> tank(m).getCapacity() == 32_000)
                .check("and should not change what is stored", m -> tank(m).getStored() == 16_000)
                .with(m -> chemicalTrait(m).capacity.set(4_000L))
                .check("shrinking below the contents must spill the excess",
                        m -> tank(m).getStored() == 4_000)
                .assertPersistenceRoundTrip()
                .check("the capacity override should survive a save/load cycle",
                        m -> tank(m).getCapacity() == 4_000)
                .check("and the tank must not come back over-full", m -> tank(m).getStored() <= 4_000)
                .with(m -> chemicalTrait(m).capacity.clear())
                .check("clearing should go back to the definition", m -> tank(m).getCapacity() == 16_000)
                .succeed();
    }

    /**
     * Switching a filter <b>on</b> over a definition that has it off — the direction that catches the
     * settings object's own {@code enable} short-circuit. The fixture configures no filter at all, so an
     * enabled empty whitelist should reject everything.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void chemical_filter_enable_override_switches_a_filter_on(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ChemicalTankTraitFixtures.MACHINE_ID, POS)
                .check("with no filter authored, anything is valid",
                        m -> tank(m).isValid(hydrogen(1)))
                .with(m -> chemicalTrait(m).filterEnabled.set(true))
                .check("an enabled empty whitelist should reject everything",
                        m -> !tank(m).isValid(hydrogen(1)))
                .with(m -> chemicalTrait(m).filterEnabled.clear())
                .check("clearing should put it back", m -> tank(m).isValid(hydrogen(1)))
                .succeed();
    }

    private static ChemicalStack hydrogen(long amount) {
        return new ChemicalStack(MekanismChemicals.HYDROGEN, amount);
    }

    private static ChemicalStorage tank(MBDMachine machine) {
        return chemicalTrait(machine).storages[0];
    }

    private static ChemicalTankCapabilityTrait chemicalTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof ChemicalTankCapabilityTrait chemicalTrait) return chemicalTrait;
        }
        throw new AssertionError("fixture machine has no chemical tank trait");
    }
}
