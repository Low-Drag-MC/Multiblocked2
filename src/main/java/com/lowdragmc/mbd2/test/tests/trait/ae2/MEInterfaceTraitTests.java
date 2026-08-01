package com.lowdragmc.mbd2.test.tests.trait.ae2;

import appeng.api.AECapabilities;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.ae2.trait.AEInterfaceSlot;
import com.lowdragmc.mbd2.integration.ae2.trait.MEInterfaceTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: NeoForge's annotation scan force-loads every @GameTestHolder class
// (Class.forName + getDeclaredMethods), which NoClassDefFoundErrors when AE2 is absent.
// Registered instead via MBDTestRegistry#onRegisterGameTests, guarded by ModList.isLoaded("ae2").
public class MEInterfaceTraitTests {
    static { @SuppressWarnings("unused") var ignored = MEInterfaceTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_interface_item_input_can_be_split_across_slots(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEInterfaceTraitFixtures.MACHINE_ID, POS)
                .insertEnergy(10_000);
        var inventory = interfaceInventory(h, scenario);
        inventory.setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 15));
        inventory.setStack(2, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 15));

        scenario.runTicks(40)
                .assertItem(0, new ItemStack(Items.EMERALD))
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_interface_fluid_input_can_be_split_across_slots(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEInterfaceTraitFixtures.MACHINE_ID, POS)
                .insertEnergy(10_000);
        var inventory = interfaceInventory(h, scenario);
        inventory.setStack(1, new GenericStack(AEFluidKey.of(Fluids.WATER), 500));
        inventory.setStack(3, new GenericStack(AEFluidKey.of(Fluids.WATER), 500));

        scenario.runTicks(40)
                .assertItem(0, new ItemStack(Items.DIAMOND))
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_interface_item_capacity_clamps_storage_slot(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEInterfaceTraitFixtures.CAPPED_MACHINE_ID, POS);
        var inventory = interfaceInventory(h, scenario);

        inventory.setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64));

        var stored = inventory.getStack(0);
        if (stored == null || stored.amount() != 16) {
            h.fail("Expected item capacity to clamp to 16, got " + stored);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_interface_fluid_capacity_clamps_storage_slot(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEInterfaceTraitFixtures.CAPPED_MACHINE_ID, POS);
        var inventory = interfaceInventory(h, scenario);

        inventory.setStack(1, new GenericStack(AEFluidKey.of(Fluids.WATER), 12_000));

        var stored = inventory.getStack(1);
        if (stored == null || stored.amount() != 8000) {
            h.fail("Expected fluid capacity to clamp to 8000, got " + stored);
            return;
        }
        h.succeed();
    }

    /**
     * Issue #221: the configured fluid capacity has to reach the slot the player actually sees.
     * The displayed tank capacity comes from the storage inventory, and the phantom (config)
     * slot's upper bound has to follow the same number instead of a hardcoded 4 buckets.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_interface_fluid_slot_uses_configured_capacity(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEInterfaceTraitFixtures.CAPPED_MACHINE_ID, POS);
        var trait = interfaceTrait(h, scenario);
        var logic = trait.getInterfaceLogic();

        // tank slot 0 of the interface is storage index 1 (item, fluid, item, fluid, ...)
        var tank = AEInterfaceSlot.createAEFluidHandler(logic.getStorage(), 1);
        if (tank.getTankCapacity(0) != 8000) {
            h.fail("Displayed tank capacity is " + tank.getTankCapacity(0) + ", expected the configured 8000");
            return;
        }
        if (AEInterfaceSlot.maxPhantomFluidAmount(logic) != 8000) {
            h.fail("Phantom fluid amount is capped at " + AEInterfaceSlot.maxPhantomFluidAmount(logic)
                    + ", expected the configured 8000");
            return;
        }
        if (AEInterfaceSlot.maxPhantomItemAmount(logic) != 16) {
            h.fail("Phantom item amount is capped at " + AEInterfaceSlot.maxPhantomItemAmount(logic)
                    + ", expected the configured 16");
            return;
        }
        h.succeed();
    }

    /** An unbound slot (no interface logic) must keep working off the AE2 defaults. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_interface_phantom_bounds_fall_back_without_logic(GameTestHelper h) {
        if (AEInterfaceSlot.maxPhantomFluidAmount(null) != 4000
                || AEInterfaceSlot.maxPhantomItemAmount(null) != 64) {
            h.fail("Unbound phantom bounds should fall back to 4000 mB / 64 items");
            return;
        }
        h.succeed();
    }

    private static MEInterfaceTrait interfaceTrait(GameTestHelper h, MBDScenario scenario) {
        var machine = scenario.machine();
        if (machine != null) {
            for (var trait : machine.getAdditionalTraits()) {
                if (trait instanceof MEInterfaceTrait interfaceTrait) {
                    return interfaceTrait;
                }
            }
        }
        h.fail("No MEInterfaceTrait on " + MEInterfaceTraitFixtures.CAPPED_MACHINE_ID);
        throw new AssertionError();
    }

    private static GenericInternalInventory interfaceInventory(GameTestHelper h, MBDScenario scenario) {
        var inventory = MBDTestHelper.capability(h, scenario.machine(), AECapabilities.GENERIC_INTERNAL_INV);
        if (inventory == null) {
            h.fail("No AE2 generic internal inventory on " + MEInterfaceTraitFixtures.MACHINE_ID);
            throw new AssertionError();
        }
        return inventory;
    }
}
