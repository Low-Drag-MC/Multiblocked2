package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@GameTestHolder(MBD2.MOD_ID)
public class FluidTankTraitTests {
    static { @SuppressWarnings("unused") var ignored = FluidTankTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_handler_capability_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidTankTraitFixtures.MACHINE_ID, POS)
                .assertExposes(Capabilities.FluidHandler.BLOCK, null)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fill_tank_then_assert(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidTankTraitFixtures.MACHINE_ID, POS)
                .insertFluid(new FluidStack(Fluids.WATER, 4000))
                .assertFluid(0, new FluidStack(Fluids.WATER, 4000))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void persistence_preserves_fluid(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidTankTraitFixtures.MACHINE_ID, POS)
                .insertFluid(new FluidStack(Fluids.WATER, 4000))
                .assertPersistenceRoundTrip()
                .assertFluid(0, new FluidStack(Fluids.WATER, 4000))
                .succeed();
    }

    /**
     * The trait's GUI IO decides which way a player may move fluid by clicking its slot with a container: IN
     * lets them empty a bucket into the tank, OUT lets them fill one from it. The slot's click switches are
     * what LDLib2's {@code FluidSlot} consults, so they are what is checked — the click itself is a client RPC
     * with a carried item, which no headless test can send.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void gui_io_gates_clicking_fluid_in_and_out(GameTestHelper h) {
        record Case(ResourceLocation machine, BlockPos pos, boolean canPourIn, boolean canTakeOut) {}
        var cases = List.of(
                new Case(FluidTankTraitFixtures.GUI_IN_MACHINE_ID, new BlockPos(0, 1, 1), true, false),
                new Case(FluidTankTraitFixtures.GUI_OUT_MACHINE_ID, new BlockPos(1, 1, 1), false, true),
                new Case(FluidTankTraitFixtures.GUI_BOTH_MACHINE_ID, new BlockPos(2, 1, 1), true, true));
        for (var c : cases) {
            var slot = fluidSlot(h, c.machine(), c.pos());
            if (slot == null) return;
            if (slot.isAllowClickDrained() != c.canPourIn() || slot.isAllowClickFilled() != c.canTakeOut()) {
                h.fail(c.machine().getPath() + ": pouring in " + slot.isAllowClickDrained() + ", taking out "
                        + slot.isAllowClickFilled() + ", expected " + c.canPourIn() + ", " + c.canTakeOut());
                return;
            }
        }
        h.succeed();
    }

    /** Places the machine and builds its UI the way opening the menu would, then finds the first tank's slot. */
    @Nullable
    private static FluidSlot fluidSlot(GameTestHelper h, ResourceLocation machineId, BlockPos pos) {
        var machine = MBDScenario.of(h).placeMachine(machineId, pos).machine();
        var state = h.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockUIMenuType.BlockUI blockUI)) {
            h.fail("the machine block does not open a ui at all");
            return null;
        }
        var holder = new BlockUIMenuType.BlockUIHolder(blockUI, h.makeMockPlayer(GameType.CREATIVE), h.absolutePos(pos), state);
        var modular = machine.createUI(holder);
        var slot = modular == null ? null : modular.ui.selectId("ui:fluid_tank_0", FluidSlot.class).findFirst().orElse(null);
        if (slot == null) {
            h.fail(machineId.getPath() + " built no fluid slot for its tank");
        }
        return slot;
    }
}
