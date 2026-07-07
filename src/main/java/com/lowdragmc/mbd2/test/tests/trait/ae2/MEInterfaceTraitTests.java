package com.lowdragmc.mbd2.test.tests.trait.ae2;

import appeng.api.AECapabilities;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.lowdragmc.mbd2.MBD2;
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

    private static GenericInternalInventory interfaceInventory(GameTestHelper h, MBDScenario scenario) {
        var inventory = MBDTestHelper.capability(h, scenario.machine(), AECapabilities.GENERIC_INTERNAL_INV);
        if (inventory == null) {
            h.fail("No AE2 generic internal inventory on " + MEInterfaceTraitFixtures.MACHINE_ID);
            throw new AssertionError();
        }
        return inventory;
    }
}
