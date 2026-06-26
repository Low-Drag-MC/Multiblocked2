package com.lowdragmc.mbd2.test.tests.editor;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.MultiblockAreaSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class MultiblockAreaSelectionTests {
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void inside_hit_calculates_min_relative_offset(GameTestHelper helper) {
        var result = MultiblockAreaSelection.pick(
                new BlockPos(10, 20, 30),
                new BlockPos(14, 24, 34),
                new BlockPos(12, 21, 34),
                Direction.SOUTH).orElseThrow();

        if (!result.offset().equals(new BlockPos(2, 1, 4)) || result.face() != Direction.SOUTH) {
            helper.fail("Unexpected controller selection result: " + result);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void reversed_area_corners_use_component_minimum(GameTestHelper helper) {
        var result = MultiblockAreaSelection.pick(
                new BlockPos(14, 24, 34),
                new BlockPos(10, 20, 30),
                new BlockPos(11, 23, 32),
                Direction.WEST).orElseThrow();

        if (!result.offset().equals(new BlockPos(1, 3, 2))) {
            helper.fail("Reversed bounds produced wrong offset: " + result.offset());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void inclusive_min_and_max_boundaries_are_accepted(GameTestHelper helper) {
        var from = new BlockPos(-2, 4, 8);
        var to = new BlockPos(2, 6, 10);

        var min = MultiblockAreaSelection.pick(from, to, from, Direction.DOWN);
        var max = MultiblockAreaSelection.pick(from, to, to, Direction.UP);
        if (min.isEmpty() || max.isEmpty()
                || !min.orElseThrow().offset().equals(BlockPos.ZERO)
                || !max.orElseThrow().offset().equals(new BlockPos(4, 2, 2))) {
            helper.fail("Inclusive area boundaries were not accepted");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void outside_hit_is_rejected(GameTestHelper helper) {
        var result = MultiblockAreaSelection.pick(
                BlockPos.ZERO,
                new BlockPos(2, 2, 2),
                new BlockPos(3, 1, 1),
                Direction.EAST);

        if (result.isPresent()) {
            helper.fail("Outside hit must not select a controller");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void all_six_hit_faces_are_preserved(GameTestHelper helper) {
        for (var face : Direction.values()) {
            var result = MultiblockAreaSelection.pick(
                    BlockPos.ZERO,
                    new BlockPos(1, 1, 1),
                    BlockPos.ZERO,
                    face).orElseThrow();
            if (result.face() != face) {
                helper.fail("Hit face was not preserved: " + face);
                return;
            }
        }
        helper.succeed();
    }
}
