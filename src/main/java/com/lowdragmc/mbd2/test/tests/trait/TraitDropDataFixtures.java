package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

public class TraitDropDataFixtures implements TestFixtureProvider {
    /** Two kept large item slots, one item slot that is not kept, and a kept tank. */
    public static final ResourceLocation KEEP = MBD2.id("test_trait_drop_keep");
    public static final ResourceLocation NO_DROP = MBD2.id("test_trait_drop_no_items");
    public static final ResourceLocation NO_DROP_KEEP = MBD2.id("test_trait_drop_no_items_keep");
    public static final ResourceLocation KEEP_RECIPE = MBD2.id("test_trait_drop_keep_recipe");
    public static final ResourceLocation KEEP_AUTO_OUTPUT = MBD2.id("test_trait_drop_keep_auto_output");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(KEEP)
                .withItemSlots(2, IO.BOTH, TraitDropDataFixtures::keptLarge)
                .withItemSlots(1, IO.BOTH)
                .withFluidTanks(1, 4000, def -> def.setKeepDataOnDrop(true))
                .register(event);
        TestMachineBuilder.simple(NO_DROP)
                .withItemSlots(1, IO.BOTH, def -> def.setDropItems(false))
                .register(event);
        TestMachineBuilder.simple(NO_DROP_KEEP)
                .withItemSlots(1, IO.BOTH, def -> {
                    def.setDropItems(false);
                    def.setKeepDataOnDrop(true);
                })
                .register(event);
        TestMachineBuilder.simple(KEEP_RECIPE)
                .withItemSlots(1, IO.IN, TraitDropDataFixtures::keptLarge)
                .withItemSlots(1, IO.OUT, def -> {
                    def.setSlotLimit(LargeItemSlotFixtures.LIMIT);
                    def.setIgnoreItemStackLimit(true);
                })
                .withEnergy(100_000)
                .withRecipeType(LargeItemSlotFixtures.RECIPE_TYPE)
                .register(event);
        TestMachineBuilder.simple(KEEP_AUTO_OUTPUT)
                .withItemSlots(1, IO.BOTH, def -> {
                    keptLarge(def);
                    def.getAutoIO().setEnable(true);
                    def.getAutoIO().setInterval(1);
                    def.getAutoIO().setRightIO(IO.OUT);
                })
                .register(event);
    }

    private static void keptLarge(ItemSlotCapabilityTraitDefinition def) {
        def.setSlotLimit(LargeItemSlotFixtures.LIMIT);
        def.setIgnoreItemStackLimit(true);
        def.setKeepDataOnDrop(true);
    }
}
