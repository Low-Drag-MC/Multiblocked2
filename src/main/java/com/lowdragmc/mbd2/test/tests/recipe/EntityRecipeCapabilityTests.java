package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class EntityRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = EntityRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void entity_input_consumed_produces_item(GameTestHelper h) {
        // Default trait AABB = [-1,-1,-1, 2,2,2] relative to machine; a pig at (1,2,1) is inside.
        // Entity scan ticks every 20; recipe duration 20 → 80 ticks is enough headroom.
        MBDScenario.of(h)
                .placeMachine(EntityRecipeCapabilityFixtures.MACHINE_ID, POS)
                .spawnEntity(EntityType.PIG, new BlockPos(1, 2, 1), pig -> {})
                .runTicks(80)
                .assertItemCountAtLeast(1, Items.DIRT, 1)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void entity_output_spawns_chicken(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(EntityRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.COBBLESTONE))
                .runTicks(60);
        // Chicken spawns somewhere inside the OUT-trait AABB around the machine.
        h.assertEntityPresent(EntityType.CHICKEN, POS, 3.0);
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void no_recipe_without_entity(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(EntityRecipeCapabilityFixtures.MACHINE_ID, POS)
                .runTicks(60)
                .assertItem(1, ItemStack.EMPTY)
                .succeed();
    }
}
