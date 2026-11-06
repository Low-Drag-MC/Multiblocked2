package com.lowdragmc.mbd2.test.tests.trait.ae2;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.integration.ae2.trait.MEInterfaceTraitDefinition;
import com.lowdragmc.mbd2.integration.ae2.trait.MEPatternProviderTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * Machines whose UI carries AE2 trait widgets, as the editor's "generate UI" builds them: a single block, and
 * multiblock controllers holding the AE2 trait themselves (issue #249).
 */
public class AEMultiblockUIFixtures implements TestFixtureProvider {
    public static final ResourceLocation SINGLE_INTERFACE_ID = MBD2.id("test_ae2_ui_single_interface");
    public static final ResourceLocation MB_INTERFACE_ID = MBD2.id("test_ae2_ui_mb_interface");
    public static final ResourceLocation MB_PATTERN_PROVIDER_ID = MBD2.id("test_ae2_ui_mb_pattern_provider");
    public static final ResourceLocation PART_ID = MBD2.id("test_ae2_ui_part");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(SINGLE_INTERFACE_ID)
                .withTrait(meInterface())
                .withItemSlots(2, IO.OUT)
                .withTraitUI()
                .register(event);

        var part = TestMachineBuilder.simple(PART_ID)
                .withItemSlots(1, IO.IN)
                .register(event);
        multiblock(MB_INTERFACE_ID, meInterface(), part, event);
        var patternProvider = new MEPatternProviderTraitDefinition();
        patternProvider.setPatternSize(3);
        patternProvider.setSlotSize(2);
        multiblock(MB_PATTERN_PROVIDER_ID, patternProvider, part, event);
    }

    private static MEInterfaceTraitDefinition meInterface() {
        var meInterface = new MEInterfaceTraitDefinition();
        meInterface.setSlotSize(2);
        meInterface.setRecipeHandlerIO(IO.IN);
        return meInterface;
    }

    // "SCP" with the controller facing north: stone one block east, the part one block west
    private static void multiblock(ResourceLocation id, TraitDefinition trait, MBDMachineDefinition part, MBDRegistryEvent.Machine event) {
        TestMachineBuilder.multiblock(id)
                .withTrait(trait)
                .withItemSlots(2, IO.OUT)
                .withTraitUI()
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(part.block()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }
}
