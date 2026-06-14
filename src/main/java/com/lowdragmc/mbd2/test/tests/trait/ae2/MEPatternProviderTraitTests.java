package com.lowdragmc.mbd2.test.tests.trait.ae2;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.integration.ae2.trait.MEInterfaceTraitDefinition;
import com.lowdragmc.mbd2.integration.ae2.trait.MEPatternProviderTrait;
import com.lowdragmc.mbd2.integration.ae2.trait.MEPatternProviderTraitDefinition;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(MBD2.MOD_ID)
public class MEPatternProviderTraitTests {
    static { @SuppressWarnings("unused") var ignored = MEPatternProviderTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
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

    @GameTest(template = "empty_simple")
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

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void me_pattern_provider_recipe_outputs_to_return_inventory(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MEPatternProviderTraitFixtures.MACHINE_ID, POS)
                .insertEnergy(10_000);
        var trait = patternProvider(h, scenario);
        trait.getStorage().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1));

        scenario.runTicks(40);

        var output = trait.getReturnInventory().getStack(0);
        if (output == null || !(output.what() instanceof AEItemKey itemKey) || itemKey.getItem() != Items.EMERALD || output.amount() != 1) {
            h.fail("Expected recipe output in pattern provider return inventory, got " + output);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
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

    @GameTest(template = "empty_simple")
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
