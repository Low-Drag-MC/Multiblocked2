package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    public static void entity_input_count_can_be_satisfied_by_multiple_entities(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(EntityRecipeCapabilityFixtures.MULTI_ENTITY_MACHINE_ID, POS)
                .spawnEntity(EntityType.PIG, new BlockPos(1, 2, 1), pig -> {})
                .spawnEntity(EntityType.PIG, new BlockPos(2, 2, 1), pig -> {})
                .runTicks(80)
                .assertItem(0, new ItemStack(Items.EMERALD, 1))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void entity_output_spawns_chicken(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(EntityRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.COBBLESTONE));
        // Poll instead of running a fixed 100 ticks and asserting at the end. The chicken spawns at a
        // random Vec3 inside the OUT-trait AABB (default [-1,-1,-1, 2,2,2] relative, worst-case
        // diagonal ≈ 3.46) and then wanders and falls for however many ticks are left, so a fixed
        // radius was racing the bird rather than the recipe. Stopping on the tick it appears keeps the
        // tight radius meaningful. Recipe searching is async and only re-polls every 5 ticks, hence the
        // generous tick budget.
        for (int tick = 0; tick < 200 && !chickenNearMachine(h); tick++) {
            MBDTestHelper.runTicks(h, 1);
        }
        h.assertEntityPresent(EntityType.CHICKEN, POS, 4.0);
        scenario.succeed();
    }

    private static boolean chickenNearMachine(GameTestHelper h) {
        var centre = Vec3.atCenterOf(h.absolutePos(POS));
        return !h.getLevel().getEntities(EntityType.CHICKEN,
                new AABB(centre, centre).inflate(4.0), Entity::isAlive).isEmpty();
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
