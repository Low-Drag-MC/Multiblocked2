package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public class LargeItemSlotFixtures implements TestFixtureProvider {
    public static final int LIMIT = 10_000;

    public static final ResourceLocation LARGE = MBD2.id("test_large_item_slot");
    /** The toggle left off, as on every existing machine. */
    public static final ResourceLocation CAPPED = MBD2.id("test_large_item_slot_capped");
    public static final ResourceLocation HUGE = MBD2.id("test_large_item_slot_huge");
    /** The item renderer on, so the storage itself syncs to the client and not only the menu slots. */
    public static final ResourceLocation SYNCED = MBD2.id("test_large_item_slot_synced");
    public static final ResourceLocation RECIPE_MACHINE = MBD2.id("test_large_item_slot_recipe_machine");
    public static final ResourceLocation RECIPE_TYPE = MBD2.id("test_large_item_slot_recipes");
    public static final ResourceLocation SMALL_OUTPUT_MACHINE = MBD2.id("test_item_slot_small_output_machine");
    public static final ResourceLocation SMALL_OUTPUT_RECIPE_TYPE = MBD2.id("test_item_slot_small_output_recipes");
    public static final ResourceLocation WORLD_OUTPUT = MBD2.id("test_large_item_world_output");
    public static final ResourceLocation AUTO_OUTPUT = MBD2.id("test_large_item_auto_output");

    public static final int RECIPE_INPUT = 200;
    public static final int RECIPE_OUTPUT = 300;
    public static final int WORLD_OUTPUT_SPEED = 200;

    private static final AABB EAST_BLOCK = new AABB(1, 0, 0, 2, 1, 1);
    private static final AABB FAR_BLOCK = new AABB(4, 0, 0, 5, 1, 1);

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(RECIPE_TYPE)
                .recipe("large_item_slot_iron_to_gold", b -> b
                        .inputItems(Items.IRON_INGOT, RECIPE_INPUT)
                        .outputItems(Items.GOLD_INGOT, RECIPE_OUTPUT)
                        .duration(20))
                .register(event);
        TestRecipeTypeBuilder.of(SMALL_OUTPUT_RECIPE_TYPE)
                .recipe("item_slot_small_output_stone_to_cobblestone", b -> b
                        .inputItems(Items.STONE)
                        .outputItems(Items.COBBLESTONE, 32)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(LARGE)
                .withItemSlots(2, IO.BOTH, def -> large(def, LIMIT))
                .withTraitAndInventoryUI()
                .register(event);
        TestMachineBuilder.simple(CAPPED)
                .withItemSlots(1, IO.BOTH, def -> def.setSlotLimit(LIMIT))
                .register(event);
        TestMachineBuilder.simple(HUGE)
                .withItemSlots(1, IO.BOTH, def -> large(def, Integer.MAX_VALUE))
                .register(event);
        TestMachineBuilder.simple(SYNCED)
                .withItemSlots(2, IO.BOTH, def -> {
                    large(def, Integer.MAX_VALUE);
                    def.getItemRendererSettings().setEnable(true);
                })
                .withTraitAndInventoryUI()
                .register(event);

        TestMachineBuilder.simple(RECIPE_MACHINE)
                .withItemSlots(1, IO.IN, def -> large(def, LIMIT))
                .withItemSlots(1, IO.OUT, def -> large(def, LIMIT))
                .withEnergy(100_000)
                .withRecipeType(RECIPE_TYPE)
                .register(event);
        TestMachineBuilder.simple(SMALL_OUTPUT_MACHINE)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT, def -> def.setSlotLimit(16))
                .withEnergy(100_000)
                .withRecipeType(SMALL_OUTPUT_RECIPE_TYPE)
                .register(event);

        TestMachineBuilder.simple(WORLD_OUTPUT)
                .withItemSlots(1, IO.BOTH, def -> {
                    large(def, LIMIT);
                    def.getAutoWorldInput().setEnable(false);
                    def.getAutoWorldInput().setRange(FAR_BLOCK);
                    var output = def.getAutoWorldOutput();
                    output.setEnable(true);
                    output.setInterval(1);
                    output.setSpeed(WORLD_OUTPUT_SPEED);
                    output.setRange(EAST_BLOCK);
                })
                .register(event);
        TestMachineBuilder.simple(AUTO_OUTPUT)
                .withItemSlots(1, IO.BOTH, def -> {
                    large(def, LIMIT);
                    def.getAutoIO().setEnable(true);
                    def.getAutoIO().setInterval(1);
                    def.getAutoIO().setRightIO(IO.OUT);
                })
                .register(event);
    }

    private static void large(ItemSlotCapabilityTraitDefinition def, int limit) {
        def.setSlotLimit(limit);
        def.setIgnoreItemStackLimit(true);
    }
}
