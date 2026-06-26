package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.trait.AutoIO;
import com.lowdragmc.mbd2.common.trait.BulkIOState;
import com.lowdragmc.mbd2.common.trait.CapabilityIO;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class BulkIOTraitTests {
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void capability_all_updates_internal_and_every_side(GameTestHelper helper) {
        var capabilityIO = new CapabilityIO();

        capabilityIO.setAllIO(IO.IN);

        assertCapabilityIO(helper, capabilityIO, IO.IN);
        helper.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_all_updates_every_side_and_enabled_state(GameTestHelper helper) {
        var autoIO = new com.lowdragmc.mbd2.common.trait.ToggleAutoIO();

        autoIO.setAllIO(IO.OUT);
        if (!autoIO.isEnable()) {
            helper.fail("Non-NONE bulk AutoIO must enable AutoIO");
            return;
        }
        assertDirectionalIO(helper, autoIO, IO.OUT);

        autoIO.setAllIO(IO.NONE);
        if (autoIO.isEnable()) {
            helper.fail("NONE bulk AutoIO must disable AutoIO");
            return;
        }
        assertDirectionalIO(helper, autoIO, IO.NONE);
        helper.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void mixed_child_values_report_mixed_bulk_state(GameTestHelper helper) {
        var capabilityIO = new CapabilityIO();
        capabilityIO.setFrontIO(IO.IN);

        if (capabilityIO.getAllIOState() != BulkIOState.MIXED) {
            helper.fail("Different child IO values must report MIXED");
            return;
        }
        helper.succeed();
    }

    private static void assertCapabilityIO(GameTestHelper helper, CapabilityIO capabilityIO, IO expected) {
        if (capabilityIO.getIO(Direction.NORTH, null) != expected) {
            helper.fail("Internal capability IO was not updated to " + expected);
            return;
        }
        for (var side : Direction.values()) {
            if (capabilityIO.getIO(Direction.NORTH, side) != expected) {
                helper.fail("Capability IO for " + side + " was not updated to " + expected);
                return;
            }
        }
    }

    private static void assertDirectionalIO(GameTestHelper helper, AutoIO autoIO, IO expected) {
        for (var side : Direction.values()) {
            if (autoIO.getIO(Direction.NORTH, side) != expected) {
                helper.fail("AutoIO for " + side + " was not updated to " + expected);
                return;
            }
        }
    }

}
