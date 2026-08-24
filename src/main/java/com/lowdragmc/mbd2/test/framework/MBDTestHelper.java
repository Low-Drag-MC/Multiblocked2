package com.lowdragmc.mbd2.test.framework;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Procedural helpers for MBD gametests. Each method takes a {@link GameTestHelper}
 * and operates relative to that test's structure region. Failures call
 * {@link GameTestHelper#fail(String)} so a failed assertion aborts the test cleanly.
 */
public final class MBDTestHelper {
    private MBDTestHelper() {}

    // region placement

    /** Place a machine block and return its {@link MBDMachine} instance. */
    public static MBDMachine placeMachine(GameTestHelper helper, ResourceLocation definitionId, BlockPos relPos) {
        MBDMachineDefinition def = MBDRegistries.MACHINE_DEFINITIONS.get(definitionId);
        if (def == null) {
            helper.fail("Unknown machine definition: " + definitionId);
            throw new AssertionError(); // unreachable — fail() throws
        }
        BlockState state = def.block().defaultBlockState();
        helper.setBlock(relPos, state);
        return getMachine(helper, relPos);
    }

    /**
     * Place a machine block at {@code relPos} with its rotation-state property forced to
     * {@code facing}. Useful for tests that exercise pattern checks against multiple
     * controller facings.
     */
    public static MBDMachine placeMachineFacing(GameTestHelper helper, ResourceLocation definitionId, BlockPos relPos, Direction facing) {
        MBDMachineDefinition def = MBDRegistries.MACHINE_DEFINITIONS.get(definitionId);
        if (def == null) {
            helper.fail("Unknown machine definition: " + definitionId);
            throw new AssertionError();
        }
        BlockState state = def.block().defaultBlockState();
        var property = def.blockProperties().rotationState().property;
        if (property.isPresent() && property.get().getPossibleValues().contains(facing)) {
            state = state.setValue(property.get(), facing);
        }
        helper.setBlock(relPos, state);
        return getMachine(helper, relPos);
    }

    /** Lookup the {@link MBDMachine} at {@code relPos}; fails the test if none present. */
    public static MBDMachine getMachine(GameTestHelper helper, BlockPos relPos) {
        BlockEntity be = helper.getBlockEntity(relPos);
        if (!(be instanceof IMachineBlockEntity machineBE)) {
            helper.fail("No machine block entity at " + relPos);
            throw new AssertionError();
        }
        if (!(machineBE.getMetaMachine() instanceof MBDMachine machine)) {
            helper.fail("BlockEntity at " + relPos + " is not an MBDMachine");
            throw new AssertionError();
        }
        return machine;
    }

    /** Place blocks per a pattern, anchored at {@code originRel} (relative to the test region). */
    public static void placePatternBlocks(GameTestHelper helper, BlockPos originRel, BlockState[][][] blocks) {
        for (int y = 0; y < blocks.length; y++) {
            for (int z = 0; z < blocks[y].length; z++) {
                for (int x = 0; x < blocks[y][z].length; x++) {
                    BlockState state = blocks[y][z][x];
                    if (state == null || state.isAir()) continue;
                    helper.setBlock(originRel.offset(x, y, z), state);
                }
            }
        }
    }

    // endregion

    // region recipes

    /**
     * A throwaway recipe with no contents and no conditions. Use it when calling
     * {@link com.lowdragmc.mbd2.api.recipe.RecipeCondition#test} directly, where the recipe
     * argument is only there to satisfy the signature.
     */
    public static MBDRecipe dummyRecipe() {
        return new MBDRecipe(null, MBD2.id("dummy"), new HashMap<>(), new HashMap<>(),
                new ArrayList<>(), new CompoundTag(), 1, false, 0);
    }

    // endregion

    // region multiblock

    /** Run a synchronous pattern check + formation on a multiblock controller. */
    public static boolean tryForm(MBDMachine machine) {
        if (!(machine instanceof MBDMultiblockMachine multiblock)) return false;
        if (!multiblock.checkPatternWithLock()) return false;
        multiblock.onStructureFormed();
        if (multiblock.isFormed() && multiblock.getLevel() instanceof ServerLevel serverLevel) {
            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
            mwsd.addMapping(multiblock.getMultiblockState());
            mwsd.removeAsyncLogic(multiblock);
        }
        return multiblock.isFormed();
    }

    public static void assertFormed(GameTestHelper helper, MBDMachine machine) {
        if (!(machine instanceof IMultiController multiblock)) {
            helper.fail("Machine " + machine.getDefinition().id() + " is not a multiblock controller");
            return;
        }
        if (!multiblock.isFormed()) helper.fail("Multiblock at " + machine.getPos() + " is not formed");
    }

    public static void assertNotFormed(GameTestHelper helper, MBDMachine machine) {
        if (machine instanceof IMultiController multiblock && multiblock.isFormed()) {
            helper.fail("Multiblock at " + machine.getPos() + " unexpectedly formed");
        }
    }

    // endregion

    // region ingredient I/O (uses block capabilities; null direction = internal access)

    public static ItemStack insertItem(GameTestHelper helper, MBDMachine machine, int slot, ItemStack stack) {
        IItemHandler handler = capability(helper, machine, Capabilities.ItemHandler.BLOCK);
        if (handler == null) {
            helper.fail("Machine " + machine.getDefinition().id() + " has no IItemHandler");
            return stack;
        }
        return handler.insertItem(slot, stack.copy(), false);
    }

    public static ItemStack extractItem(GameTestHelper helper, MBDMachine machine, int slot, int amount) {
        IItemHandler handler = capability(helper, machine, Capabilities.ItemHandler.BLOCK);
        if (handler == null) {
            helper.fail("Machine " + machine.getDefinition().id() + " has no IItemHandler");
            return ItemStack.EMPTY;
        }
        return handler.extractItem(slot, amount, false);
    }

    public static int insertFluid(GameTestHelper helper, MBDMachine machine, FluidStack stack) {
        IFluidHandler handler = capability(helper, machine, Capabilities.FluidHandler.BLOCK);
        if (handler == null) {
            helper.fail("Machine " + machine.getDefinition().id() + " has no IFluidHandler");
            return 0;
        }
        return handler.fill(stack.copy(), IFluidHandler.FluidAction.EXECUTE);
    }

    public static int insertEnergy(GameTestHelper helper, MBDMachine machine, int amount) {
        IEnergyStorage storage = capability(helper, machine, Capabilities.EnergyStorage.BLOCK);
        if (storage == null) {
            helper.fail("Machine " + machine.getDefinition().id() + " has no IEnergyStorage");
            return 0;
        }
        return storage.receiveEnergy(amount, false);
    }

    // endregion

    // region capability lookup

    @Nullable
    public static <T> T capability(GameTestHelper helper, MBDMachine machine, BlockCapability<T, @Nullable Direction> cap) {
        return capabilityAt(helper, machine.getPos(), cap, null);
    }

    @Nullable
    public static <T> T capability(GameTestHelper helper, BlockPos relPos, BlockCapability<T, @Nullable Direction> cap, @Nullable Direction side) {
        return capabilityAt(helper, helper.absolutePos(relPos), cap, side);
    }

    @Nullable
    private static <T> T capabilityAt(GameTestHelper helper, BlockPos absPos, BlockCapability<T, @Nullable Direction> cap, @Nullable Direction side) {
        ServerLevel level = helper.getLevel();
        BlockEntity be = level.getBlockEntity(absPos);
        BlockState state = level.getBlockState(absPos);
        return cap.getCapability(level, absPos, state, be, side);
    }

    public static <T> void assertCapability(GameTestHelper helper, BlockPos relPos, BlockCapability<T, @Nullable Direction> cap, @Nullable Direction side, Predicate<T> check) {
        T value = capability(helper, relPos, cap, side);
        if (value == null) {
            helper.fail("Capability " + cap.name() + " not exposed at " + relPos + " side=" + side);
            return;
        }
        if (!check.test(value)) {
            helper.fail("Capability " + cap.name() + " at " + relPos + " side=" + side + " failed predicate");
        }
    }

    // endregion

    // region assertions on machine state

    /**
     * Asserts the slot contains a damageable item of the given type with damage value at least {@code minDamage}.
     * Use this for durability-recipe tests where the exact damage may vary by how many cycles ran.
     */
    public static void assertItemDamageAtLeast(GameTestHelper helper, MBDMachine machine, int slot, net.minecraft.world.item.Item item, int minDamage) {
        IItemHandler handler = capability(helper, machine, Capabilities.ItemHandler.BLOCK);
        if (handler == null) {
            helper.fail("No IItemHandler on " + machine.getDefinition().id());
            return;
        }
        ItemStack actual = handler.getStackInSlot(slot);
        if (actual.getItem() != item) {
            helper.fail("Expected " + item + " in slot " + slot + " of " + machine.getDefinition().id() + ", got " + actual);
            return;
        }
        if (actual.getDamageValue() < minDamage) {
            helper.fail("Expected damage >= " + minDamage + " in slot " + slot + ", got " + actual.getDamageValue());
        }
    }

    /**
     * Asserts the slot contains an item of the given type with count at least {@code minCount}, ignoring components.
     */
    public static void assertItemCountAtLeast(GameTestHelper helper, MBDMachine machine, int slot, net.minecraft.world.item.Item item, int minCount) {
        IItemHandler handler = capability(helper, machine, Capabilities.ItemHandler.BLOCK);
        if (handler == null) {
            helper.fail("No IItemHandler on " + machine.getDefinition().id());
            return;
        }
        ItemStack actual = handler.getStackInSlot(slot);
        if (actual.getItem() != item) {
            helper.fail("Expected " + item + " in slot " + slot + " of " + machine.getDefinition().id() + ", got " + actual);
            return;
        }
        if (actual.getCount() < minCount) {
            helper.fail("Expected >= " + minCount + " " + item + " in slot " + slot + ", got " + actual.getCount());
        }
    }

    public static void assertItemPresent(GameTestHelper helper, MBDMachine machine, int slot, ItemStack expected) {
        IItemHandler handler = capability(helper, machine, Capabilities.ItemHandler.BLOCK);
        if (handler == null) {
            helper.fail("No IItemHandler on " + machine.getDefinition().id());
            return;
        }
        ItemStack actual = handler.getStackInSlot(slot);
        if (!ItemStack.isSameItemSameComponents(actual, expected) || actual.getCount() < expected.getCount()) {
            helper.fail("Expected " + expected + " (components=" + expected.getComponents() + ") in slot " + slot + " of " + machine.getDefinition().id()
                    + ", got " + actual + " (components=" + actual.getComponents() + ")");
        }
    }

    public static void assertFluidPresent(GameTestHelper helper, MBDMachine machine, int tank, FluidStack expected) {
        IFluidHandler handler = capability(helper, machine, Capabilities.FluidHandler.BLOCK);
        if (handler == null) {
            helper.fail("No IFluidHandler on " + machine.getDefinition().id());
            return;
        }
        FluidStack actual = handler.getFluidInTank(tank);
        if (!FluidStack.isSameFluidSameComponents(actual, expected) || actual.getAmount() < expected.getAmount()) {
            helper.fail("Expected " + expected + " in tank " + tank + " of " + machine.getDefinition().id() + ", got " + actual);
        }
    }

    public static void assertEnergyAtLeast(GameTestHelper helper, MBDMachine machine, int amount) {
        IEnergyStorage storage = capability(helper, machine, Capabilities.EnergyStorage.BLOCK);
        if (storage == null) {
            helper.fail("No IEnergyStorage on " + machine.getDefinition().id());
            return;
        }
        if (storage.getEnergyStored() < amount) {
            helper.fail("Expected >= " + amount + " FE in " + machine.getDefinition().id() + ", got " + storage.getEnergyStored());
        }
    }

    public static void assertMachineState(GameTestHelper helper, MBDMachine machine, String expectedState) {
        String actual = machine.getMachineStateName();
        if (!expectedState.equals(actual)) {
            helper.fail("Machine state expected " + expectedState + " but was " + actual);
        }
    }

    // endregion

    // region persistence

    /**
     * Save the BE at {@code relPos} to NBT, replace it with a fresh BE of the same type,
     * then deserialize. Returns the new {@link MBDMachine} instance. Fails the test if
     * the machine no longer exists after the round trip.
     */
    public static MBDMachine roundTripPersistence(GameTestHelper helper, BlockPos relPos) {
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(relPos);
        BlockEntity be = level.getBlockEntity(absPos);
        if (be == null) {
            helper.fail("No BlockEntity at " + relPos + " to round-trip");
            throw new AssertionError();
        }
        var provider = level.registryAccess();
        CompoundTag saved = be.saveWithFullMetadata(provider);
        BlockState state = level.getBlockState(absPos);

        // Mark the existing BE as removed and detach it
        be.setRemoved();
        level.removeBlockEntity(absPos);

        BlockEntity fresh = state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb
                ? eb.newBlockEntity(absPos, state)
                : null;
        if (fresh == null) {
            helper.fail("Could not recreate BlockEntity at " + relPos);
            throw new AssertionError();
        }
        // Attach to the level first so the BE has a valid level reference, then load
        level.setBlockEntity(fresh);
        fresh.loadWithComponents(saved, provider);
        // clearRemoved triggers MBDMachine.onLoad which re-attaches the trait storages
        fresh.clearRemoved();
        return getMachine(helper, relPos);
    }

    // endregion

    // region time control

    /** Run {@code ticks} server ticks against the test's level. */
    public static void runTicks(GameTestHelper helper, int ticks) {
        for (int i = 0; i < ticks; i++) {
            helper.getLevel().tick(() -> true);
        }
    }


    // endregion

    // region volume fill / pattern placement

    /** Fill every cell in the inclusive cuboid {@code [min, max]} with {@code state}. */
    public static void fillVolume(GameTestHelper helper, BlockPos min, BlockPos max, BlockState state) {
        int x0 = Math.min(min.getX(), max.getX()), x1 = Math.max(min.getX(), max.getX());
        int y0 = Math.min(min.getY(), max.getY()), y1 = Math.max(min.getY(), max.getY());
        int z0 = Math.min(min.getZ(), max.getZ()), z1 = Math.max(min.getZ(), max.getZ());
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    helper.setBlock(new BlockPos(x, y, z), state);
                }
            }
        }
    }

    // endregion

    // region neighbor inventory

    /** Place a vanilla chest at {@code relPos} and stuff {@code items} into its inventory slots in order. */
    public static void placeChestWithItems(GameTestHelper helper, BlockPos relPos, ItemStack... items) {
        helper.setBlock(relPos, Blocks.CHEST.defaultBlockState());
        BlockEntity be = helper.getBlockEntity(relPos);
        if (!(be instanceof Container container)) {
            helper.fail("Expected a Container at " + relPos + ", got " + (be == null ? "null" : be.getClass()));
            return;
        }
        int max = Math.min(items.length, container.getContainerSize());
        for (int i = 0; i < max; i++) {
            container.setItem(i, items[i].copy());
        }
        be.setChanged();
    }

    /** Read the items currently in the chest (or other Container) at {@code relPos}. */
    public static ItemStack[] readChestItems(GameTestHelper helper, BlockPos relPos) {
        BlockEntity be = helper.getBlockEntity(relPos);
        if (!(be instanceof Container container)) {
            helper.fail("Expected a Container at " + relPos + ", got " + (be == null ? "null" : be.getClass()));
            return new ItemStack[0];
        }
        ItemStack[] out = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < out.length; i++) out[i] = container.getItem(i);
        return out;
    }

    // endregion

    // region entity spawning

    /** Spawn an entity at the center of {@code relPos}; {@code setup} runs after spawn to mutate it. */
    public static <T extends Entity> T spawnEntity(GameTestHelper helper, EntityType<T> type, BlockPos relPos, Consumer<T> setup) {
        BlockPos absPos = helper.absolutePos(relPos);
        ServerLevel level = helper.getLevel();
        T entity = type.create(level);
        if (entity == null) {
            helper.fail("Could not create entity of type " + type);
            throw new AssertionError();
        }
        entity.setPos(absPos.getX() + 0.5, absPos.getY(), absPos.getZ() + 0.5);
        if (setup != null) setup.accept(entity);
        level.addFreshEntity(entity);
        return entity;
    }

    // endregion
}
