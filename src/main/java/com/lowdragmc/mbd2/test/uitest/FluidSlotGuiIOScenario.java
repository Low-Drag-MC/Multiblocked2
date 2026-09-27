package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTrait;
import com.lowdragmc.mbd2.test.tests.trait.FluidTankTraitFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/**
 * Clicking a fluid tank's slot with a water bucket, for real: the fluid trait's GUI IO must decide whether
 * the bucket may be emptied into the tank.
 *
 * <p>The gametest {@code FluidTankTraitTests.gui_io_gates_clicking_fluid_in_and_out} checks the slot's
 * switches; this checks the click they are meant to stop, which is a client RPC carrying the player's held
 * item and so exists only with a client. An OUT-only tank must refuse the bucket and an IN-only one take it —
 * the second half is also what shows the click got through at all, so the first half is not passing on a
 * click that went nowhere.</p>
 *
 * <p>Run with {@code gradlew runClient -PldTest=mbd2_fluid_slot_gui_io}.</p>
 */
@LDLRegisterClient(name = "mbd2_fluid_slot_gui_io", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class FluidSlotGuiIOScenario implements UIScenario {
    /** The first tank's slot, as {@code FluidTankCapabilityTraitDefinition#createTraitUITemplate} names it. */
    private static final String SLOT_ID = "ui:fluid_tank_0";

    /** Next to the player, chosen at placement time — see MachineAutoIOPanelScenario on why not fixed. */
    private BlockPos pos = BlockPos.ZERO;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(150).tags("mbd2", "fluid", "ui").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("start from no screen", ctx -> ctx.mc().setScreen(null)).frames(2);
        clickWithWaterBucket(s, FluidTankTraitFixtures.GUI_OUT_MACHINE_ID, "an OUT-only tank");
        s.checkServer("an OUT-only tank refused the bucket", sc -> tankAmount(sc) == 0
                        && sc.player().containerMenu.getCarried().is(Items.WATER_BUCKET))
                .screenshot("gui_out_refused")
                .step("close the ui", ctx -> ctx.mc().setScreen(null))
                .frames(2);
        clickWithWaterBucket(s, FluidTankTraitFixtures.GUI_IN_MACHINE_ID, "an IN-only tank");
        // FluidSlot hands the emptied bucket to the inventory, leaving the cursor empty
        s.checkServer("an IN-only tank took the bucket", sc -> tankAmount(sc) == 1000
                        && sc.player().containerMenu.getCarried().isEmpty())
                .screenshot("gui_in_accepted")
                .step("close the ui", ctx -> ctx.mc().setScreen(null))
                // the dev world persists, so give back what the run left: the water bucket still held when the
                // OUT-only ui closed, the empty one the IN-only tank handed over, and the machine
                .teardownServer("clean up", sc -> {
                    var player = sc.player();
                    player.closeContainer();
                    var craftSlots = player.inventoryMenu.getCraftSlots();
                    player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.WATER_BUCKET), 1, craftSlots);
                    player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.BUCKET), 1, craftSlots);
                    sc.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                });
    }

    private void clickWithWaterBucket(ScenarioBuilder s, ResourceLocation machineId, String what) {
        s.server("place " + what, sc -> {
                    var definition = MBDRegistries.MACHINE_DEFINITIONS.get(machineId);
                    if (definition == null) {
                        throw new IllegalStateException(machineId + " is not registered");
                    }
                    pos = sc.player().blockPosition().above(2);
                    sc.level().setBlockAndUpdate(pos, definition.block().defaultBlockState());
                })
                .waitUntil("the client to have the machine", ctx ->
                        ctx.level() != null && ctx.level().getBlockEntity(pos) instanceof IMachineBlockEntity)
                .server("open its ui the way a player would", sc -> BlockUIMenuType.openUI(sc.player(), pos))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(4)
                .server("hold a water bucket", sc -> sc.player().containerMenu.setCarried(new ItemStack(Items.WATER_BUCKET)))
                .serverTicks(2)
                .step("click the tank's slot", ctx -> {
                    var slot = ctx.query().withId(SLOT_ID).one().bounds();
                    ctx.input().mouseDown(slot.centerX(), slot.centerY(), 0);
                    ctx.input().mouseUp(slot.centerX(), slot.centerY(), 0);
                })
                .serverTicks(4);
    }

    private int tankAmount(ServerContext sc) {
        if (sc.level().getBlockEntity(pos) instanceof IMachineBlockEntity be && be.getMetaMachine() instanceof MBDMachine machine) {
            for (var trait : machine.getAdditionalTraits()) {
                if (trait instanceof FluidTankCapabilityTrait tank) {
                    var fluid = tank.storages[0].getFluid();
                    return fluid.isEmpty() ? 0 : fluid.is(Fluids.WATER) ? fluid.getAmount() : -1;
                }
            }
        }
        throw new AssertionError("no fluid tank machine at " + pos);
    }
}
