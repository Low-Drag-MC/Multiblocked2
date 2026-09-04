package com.lowdragmc.mbd2.test.tests.trait.ae2;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.integration.ae2.trait.MEInterfaceTraitDefinition;
import com.lowdragmc.mbd2.integration.ae2.trait.MEPatternProviderTrait;
import com.lowdragmc.mbd2.integration.ae2.trait.MEPatternProviderTraitDefinition;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

// No @GameTestHolder: NeoForge's annotation scan force-loads every @GameTestHolder class
// (Class.forName + getDeclaredMethods), which NoClassDefFoundErrors when AE2 is absent.
// Registered instead via MBDTestRegistry#onRegisterGameTests, guarded by ModList.isLoaded("ae2").
public class MEPatternProviderTraitTests {
    static { @SuppressWarnings("unused") var ignored = MEPatternProviderTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_pattern_provider_item_capacity_clamps_storage_slot(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEPatternProviderTraitFixtures.MACHINE_ID, POS);
        var trait = patternProvider(h, scenario);

        trait.getStorage().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64));

        var stored = trait.getStorage().getStack(0);
        if (stored == null || stored.amount() != 16) {
            h.fail("Expected item capacity to clamp to 16, got " + stored);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_pattern_provider_fluid_capacity_clamps_storage_slot(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEPatternProviderTraitFixtures.MACHINE_ID, POS);
        var trait = patternProvider(h, scenario);

        trait.getStorage().setStack(1, new GenericStack(AEFluidKey.of(Fluids.WATER), 12_000));

        var stored = trait.getStorage().getStack(1);
        if (stored == null || stored.amount() != 8000) {
            h.fail("Expected fluid capacity to clamp to 8000, got " + stored);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_pattern_provider_recipe_outputs_to_return_inventory(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEPatternProviderTraitFixtures.MACHINE_ID, POS)
                .insertEnergy(10_000);
        var trait = patternProvider(h, scenario);
        trait.getStorage().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1));

        // Poll rather than budget a fixed 40 ticks: recipe searching runs on a background thread and is
        // only re-polled every 5 ticks, so a fixed budget races it. Batch composition changes the timing,
        // which is why this failed intermittently once unrelated tests were added.
        for (int tick = 0; tick < 200 && !hasEmerald(trait); tick++) {
            MBDTestHelper.runTicks(h, 1);
        }

        var output = trait.getReturnInventory().getStack(0);
        if (output == null || !(output.what() instanceof AEItemKey itemKey) || itemKey.getItem() != Items.EMERALD || output.amount() != 1) {
            h.fail("Expected recipe output in pattern provider return inventory, got " + output);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void me_pattern_provider_and_interface_definitions_are_mutually_exclusive(GameTestHelper h) {
        var interfaceDefinition = new MEInterfaceTraitDefinition();
        var providerDefinition = new MEPatternProviderTraitDefinition();

        if (interfaceDefinition.canBeAddedTo(List.of(providerDefinition))) {
            h.fail("Expected ME interface trait definition to reject existing ME pattern provider trait");
            return;
        }
        if (providerDefinition.canBeAddedTo(List.of(interfaceDefinition))) {
            h.fail("Expected ME pattern provider trait definition to reject existing ME interface trait");
            return;
        }
        h.succeed();
    }

    /** Non-failing read, for polling: the assertion afterwards is what reports a real failure. */
    private static boolean hasEmerald(MEPatternProviderTrait trait) {
        var stack = trait.getReturnInventory().getStack(0);
        return stack != null && stack.what() instanceof AEItemKey key && key.getItem() == Items.EMERALD;
    }

    private static MEPatternProviderTrait patternProvider(GameTestHelper h, MBDScenario scenario) {
        var machine = scenario.machine();
        if (machine == null) {
            h.fail("No current machine");
            throw new AssertionError();
        }
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof MEPatternProviderTrait providerTrait) {
                return providerTrait;
            }
        }
        h.fail("No ME pattern provider trait on " + MEPatternProviderTraitFixtures.MACHINE_ID);
        throw new AssertionError();
    }

    /**
     * The buffer capacities as runtime values. {@code applyCapacities()} already existed and is already
     * re-appliable, so the slots only had to read from themselves and hook it — but the hook is what
     * makes an override reach AE2's inventories, which cache the capacity per key type.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pattern_provider_capacity_overrides_reach_the_inventories(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(MEPatternProviderTraitFixtures.MACHINE_ID, POS)
                .check("the buffers start on the definition",
                        m -> itemBufferCapacity(m) == 16 && fluidBufferCapacity(m) == 8000)
                .with(m -> providerTrait(m).itemCapacity.set(64))
                .with(m -> providerTrait(m).fluidCapacity.set(16_000))
                .check("an override should reach the storage inventory",
                        m -> itemBufferCapacity(m) == 64 && fluidBufferCapacity(m) == 16_000)
                .assertPersistenceRoundTrip()
                .check("and survive a save/load cycle",
                        m -> itemBufferCapacity(m) == 64 && fluidBufferCapacity(m) == 16_000)
                .with(m -> {
                    providerTrait(m).itemCapacity.clear();
                    providerTrait(m).fluidCapacity.clear();
                })
                .check("clearing should go back to the definition",
                        m -> itemBufferCapacity(m) == 16 && fluidBufferCapacity(m) == 8000)
                .succeed();
    }

    private static long itemBufferCapacity(MBDMachine machine) {
        return providerTrait(machine).getPatternProviderLogic().getReturnInventory().getCapacity(AEKeyType.items());
    }

    private static long fluidBufferCapacity(MBDMachine machine) {
        return providerTrait(machine).getPatternProviderLogic().getReturnInventory().getCapacity(AEKeyType.fluids());
    }

    private static MEPatternProviderTrait providerTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof MEPatternProviderTrait providerTrait) return providerTrait;
        }
        throw new AssertionError("fixture machine has no ME pattern provider trait");
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void machine_settings_rejects_adding_pattern_provider_and_interface_together(GameTestHelper h) {
        var settings = ConfigMachineSettings.builder().build();
        var interfaceDefinition = new MEInterfaceTraitDefinition();
        var providerDefinition = new MEPatternProviderTraitDefinition();

        if (!settings.addTraitDefinition(interfaceDefinition)) {
            h.fail("Expected first ME trait definition to be accepted");
            return;
        }
        if (settings.addTraitDefinition(providerDefinition)) {
            h.fail("Expected machine settings to reject incompatible ME pattern provider trait");
            return;
        }
        if (settings.traitDefinitions().size() != 1) {
            h.fail("Expected only one ME trait definition to be stored, got " + settings.traitDefinitions().size());
            return;
        }
        h.succeed();
    }
}
