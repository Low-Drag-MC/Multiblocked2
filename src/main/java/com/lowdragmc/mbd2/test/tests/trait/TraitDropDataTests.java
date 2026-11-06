package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.data.MBDDataComponents;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder(MBD2.MOD_ID)
public class TraitDropDataTests {
    static { @SuppressWarnings("unused") var ignored = TraitDropDataFixtures.KEEP; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos EAST = MACHINE.relative(Direction.EAST);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void without_the_option_contents_drop_beside_a_plain_item(GameTestHelper h) {
        var machine = place(h, ItemSlotTraitFixtures.MACHINE_ID, POS);
        item(machine, 0).storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 10));
        var drops = breakMachine(h, POS);
        var machineItem = machineItem(h, drops, ItemSlotTraitFixtures.MACHINE_ID);
        if (machineItem == null) return;
        if (machineItem.has(MBDDataComponents.TRAIT_DATA)) {
            h.fail("a machine without the option kept data: " + machineItem.get(MBDDataComponents.TRAIT_DATA));
            return;
        }
        if (count(drops, Items.IRON_INGOT) != 10) {
            h.fail("dropped " + count(drops, Items.IRON_INGOT) + " of 10 iron");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void kept_traits_come_back_when_placed(GameTestHelper h) {
        var machine = place(h, TraitDropDataFixtures.KEEP, POS);
        var named = new ItemStack(Items.GOLD_INGOT, 3);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("kept"));
        item(machine, 0).storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 5000));
        item(machine, 0).storage.setStackInSlot(1, named.copy());
        item(machine, 0).slotLimit.set(6000);
        item(machine, 1).storage.setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
        fluid(machine).storages[0].setFluidInTank(new FluidStack(Fluids.WATER, 3000), false);

        var drops = breakMachine(h, POS);
        var machineItem = machineItem(h, drops, TraitDropDataFixtures.KEEP);
        if (machineItem == null) return;
        if (count(drops, Items.IRON_INGOT) + count(drops, Items.GOLD_INGOT) != 0) {
            h.fail("a kept trait dropped its items as well: " + drops);
            return;
        }
        if (count(drops, Items.DIAMOND) != 7) {
            h.fail("the trait that is not kept dropped " + count(drops, Items.DIAMOND) + " of 7 diamonds");
            return;
        }

        var placed = placeFromItem(h, POS, throughCodecs(h, machineItem));
        if (placed == null) return;
        if (!LargeItemSlotTests.expectStack(h, "kept slot 0", item(placed, 0).storage.getStackInSlot(0), new ItemStack(Items.IRON_INGOT, 5000))
                || !LargeItemSlotTests.expectStack(h, "kept slot 1", item(placed, 0).storage.getStackInSlot(1), named)
                || !LargeItemSlotTests.expectStack(h, "the slot that is not kept", item(placed, 1).storage.getStackInSlot(0), ItemStack.EMPTY)) return;
        if (!item(placed, 0).slotLimit.isOverridden() || item(placed, 0).slotLimit.get() != 6000) {
            h.fail("the kept runtime value came back as " + item(placed, 0).slotLimit.get());
            return;
        }
        var water = fluid(placed).storages[0].getFluid();
        if (!water.is(Fluids.WATER) || water.getAmount() != 3000) {
            h.fail("the kept tank came back with " + water);
            return;
        }
        MBDScenario.of(h).target(POS).assertPersistenceRoundTrip();
        if (!LargeItemSlotTests.expectStack(h, "kept slot 0 after reload",
                item(MBDTestHelper.getMachine(h, POS), 0).storage.getStackInSlot(0), new ItemStack(Items.IRON_INGOT, 5000))) return;
        h.succeed();
    }

    /** It would otherwise never stack with a new one. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void nothing_to_keep_drops_a_plain_item(GameTestHelper h) {
        place(h, TraitDropDataFixtures.KEEP, POS);
        var machineItem = machineItem(h, breakMachine(h, POS), TraitDropDataFixtures.KEEP);
        if (machineItem == null) return;
        var fresh = MBDRegistries.MACHINE_DEFINITIONS.get(TraitDropDataFixtures.KEEP).asStack();
        if (!ItemStack.isSameItemSameComponents(machineItem, fresh)) {
            h.fail("an empty machine dropped " + machineItem.getComponentsPatch() + ", a new one is " + fresh.getComponentsPatch());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void kept_contents_drop_when_the_machine_drops_no_item(GameTestHelper h) {
        var machine = place(h, TraitDropDataFixtures.KEEP, POS);
        machine.dropMachineItem.set(false);
        item(machine, 0).storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 150));
        var drops = breakMachine(h, POS);
        if (count(drops, Items.IRON_INGOT) != 150 || drops.stream().anyMatch(stack -> stack.getCount() > stack.getMaxStackSize())) {
            h.fail("without a machine item the kept slots dropped " + drops);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void items_can_be_set_not_to_drop(GameTestHelper h) {
        var machine = place(h, TraitDropDataFixtures.NO_DROP, POS);
        item(machine, 0).storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 10));
        var drops = breakMachine(h, POS);
        var machineItem = machineItem(h, drops, TraitDropDataFixtures.NO_DROP);
        if (machineItem == null) return;
        if (drops.size() != 1 || machineItem.has(MBDDataComponents.TRAIT_DATA)) {
            h.fail("a machine whose items do not drop dropped " + drops);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void keeping_data_still_works_for_items_that_do_not_drop(GameTestHelper h) {
        var machine = place(h, TraitDropDataFixtures.NO_DROP_KEEP, POS);
        item(machine, 0).storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 10));
        var machineItem = machineItem(h, breakMachine(h, POS), TraitDropDataFixtures.NO_DROP_KEEP);
        if (machineItem == null) return;
        var placed = placeFromItem(h, POS, machineItem);
        if (placed != null && LargeItemSlotTests.expectStack(h, "the kept slot",
                item(placed, 0).storage.getStackInSlot(0), new ItemStack(Items.IRON_INGOT, 10))) h.succeed();
    }

    /** Runtime values are never swept, so only marking them gets them saved. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void restored_data_gets_the_machine_saved(GameTestHelper h) {
        var source = place(h, TraitDropDataFixtures.KEEP, POS);
        item(source, 0).slotLimit.set(6000);
        var machineItem = machineItem(h, breakMachine(h, POS), TraitDropDataFixtures.KEEP);
        if (machineItem == null) return;
        MBDScenario.of(h).placeMachine(TraitDropDataFixtures.KEEP, POS)
                .assertChangeGetsSaved("restoring a runtime value", m -> m.onMachinePlaced(null, machineItem))
                .check("the runtime value came back", m -> item(m, 0).slotLimit.get() == 6000)
                .succeed();
    }

    /** A machine that already looked at its slots and found them empty has to look again. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_running_machine_uses_restored_items(GameTestHelper h) {
        var source = place(h, TraitDropDataFixtures.KEEP_AUTO_OUTPUT, POS);
        item(source, 0).storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 200));
        var machineItem = machineItem(h, breakMachine(h, POS), TraitDropDataFixtures.KEEP_AUTO_OUTPUT);
        if (machineItem == null) return;
        MBDTestHelper.placeChestWithItems(h, EAST);
        MBDScenario.of(h).placeMachineFacing(TraitDropDataFixtures.KEEP_AUTO_OUTPUT, MACHINE, Direction.NORTH)
                .runTicks(3)
                .with(m -> m.onMachinePlaced(null, machineItem))
                .runUntil(m -> chestCount(h) >= 128, 40)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void restored_inputs_get_processed(GameTestHelper h) {
        var source = place(h, TraitDropDataFixtures.KEEP_RECIPE, POS);
        item(source, 0).storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, LargeItemSlotFixtures.RECIPE_INPUT));
        var machineItem = machineItem(h, breakMachine(h, POS), TraitDropDataFixtures.KEEP_RECIPE);
        if (machineItem == null) return;
        MBDScenario.of(h).placeMachine(TraitDropDataFixtures.KEEP_RECIPE, POS)
                .insertEnergy(10_000)
                .runTicks(10)
                .with(m -> m.onMachinePlaced(null, machineItem))
                .runUntil(m -> item(m, 1).storage.getStackInSlot(0).getCount() >= LargeItemSlotFixtures.RECIPE_OUTPUT, 200)
                .succeed();
    }

    /** Traits renamed or removed since the item was dropped. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void data_for_unknown_traits_is_ignored(GameTestHelper h) {
        var stack = MBDRegistries.MACHINE_DEFINITIONS.get(TraitDropDataFixtures.KEEP).asStack();
        var data = new CompoundTag();
        data.put("trait", new CompoundTag());
        data.getCompound("trait").put("missing", new CompoundTag());
        data.getCompound("trait").getCompound("missing").putInt("storage", 5);
        stack.set(MBDDataComponents.TRAIT_DATA, CustomData.of(data));
        var placed = placeFromItem(h, POS, stack);
        if (placed != null && LargeItemSlotTests.expectStack(h, "a slot of the placed machine",
                item(placed, 0).storage.getStackInSlot(0), ItemStack.EMPTY)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void an_item_with_data_says_so(GameTestHelper h) {
        var machine = place(h, TraitDropDataFixtures.KEEP, POS);
        item(machine, 0).storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 1));
        var machineItem = machineItem(h, breakMachine(h, POS), TraitDropDataFixtures.KEEP);
        if (machineItem == null) return;
        var tooltip = new ArrayList<Component>();
        MBDRegistries.MACHINE_DEFINITIONS.get(TraitDropDataFixtures.KEEP).appendHoverText(machineItem, tooltip);
        if (tooltip.stream().noneMatch(line -> line.getContents().toString().contains("mbd2.tooltip.machine.trait_data"))) {
            h.fail("the tooltip of an item with data is " + tooltip);
            return;
        }
        h.succeed();
    }

    private static MBDMachine place(GameTestHelper h, ResourceLocation id, BlockPos pos) {
        return MBDScenario.of(h).placeMachine(id, pos).machine();
    }

    private static List<ItemStack> breakMachine(GameTestHelper h, BlockPos pos) {
        h.getLevel().destroyBlock(h.absolutePos(pos), true);
        var entities = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(1.5));
        var drops = entities.stream().map(entity -> entity.getItem().copy()).toList();
        entities.forEach(Entity::discard);
        return drops;
    }

    @Nullable
    private static ItemStack machineItem(GameTestHelper h, List<ItemStack> drops, ResourceLocation id) {
        var item = MBDRegistries.MACHINE_DEFINITIONS.get(id).asStack().getItem();
        var found = drops.stream().filter(stack -> stack.is(item)).toList();
        if (found.size() != 1) {
            h.fail("expected one machine item among " + drops);
            return null;
        }
        return found.getFirst();
    }

    /** The way a player places it, through {@link BlockItem#place}. */
    @Nullable
    private static MBDMachine placeFromItem(GameTestHelper h, BlockPos pos, ItemStack stack) {
        var abs = h.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        var result = ((BlockItem) stack.getItem())
                .place(new BlockPlaceContext(h.getLevel(), null, InteractionHand.MAIN_HAND, stack.copy(), hit));
        if (!result.consumesAction()) {
            h.fail("placing the machine item was refused: " + result);
            return null;
        }
        return MBDTestHelper.getMachine(h, pos);
    }

    /** Saved in an inventory and sent to a client, so the data must survive both codecs. */
    private static ItemStack throughCodecs(GameTestHelper h, ItemStack stack) {
        var provider = h.getLevel().registryAccess();
        var parsed = ItemStack.parse(provider, stack.save(provider)).orElseThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), provider);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, parsed);
            var decoded = ItemStack.STREAM_CODEC.decode(buffer);
            if (!ItemStack.matches(decoded, stack)) {
                h.fail("the machine item changed through its codecs: " + decoded.getComponentsPatch() + " from " + stack.getComponentsPatch());
            }
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static int count(List<ItemStack> drops, net.minecraft.world.item.Item item) {
        return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static int chestCount(GameTestHelper h) {
        var count = 0;
        for (var stack : MBDTestHelper.readChestItems(h, EAST)) count += stack.getCount();
        return count;
    }

    private static ItemSlotCapabilityTrait item(MBDMachine machine, int index) {
        return LargeItemSlotTests.traits(machine).get(index);
    }

    private static FluidTankCapabilityTrait fluid(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof FluidTankCapabilityTrait tank) return tank;
        }
        throw new AssertionError("fixture machine has no fluid tank trait");
    }
}
