package com.lowdragmc.mbd2.common.blueprint.node.machine;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.UseWithContext;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineInfoBlock;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The properties of an {@link MBDMachine}, one block each.
 *
 * <p>One class per property rather than a name-driven reflective read, for the reasons KilaGraph's
 * {@code InfoContextNode} lays out: reflection cannot tell data from plumbing, it serialises a member
 * name that a rename turns into a silent null, and it produces dead pins for types the graph does not
 * carry. A block declares its output type and converts at its own boundary.</p>
 */
public final class MachineInfoBlocks {

    private static final String GROUP = "mbd2/machine";

    private MachineInfoBlocks() {}

    /** Base for the machine blocks, so each concrete one is only its ports and its read. */
    private abstract static class MachineBlock extends MachineInfoBlock<MBDMachine> {
        @Override
        protected final Class<MBDMachine> targetClass() {
            return MBDMachine.class;
        }
    }

    // ---- placement ---------------------------------------------------------------------------

    /** The level the machine is in. */
    @NodeAttribute(name = "mbd2_machine_level", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class MachineLevel extends MachineBlock {
        @OutputPort public Level value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getLevel());
        }
    }

    /** Where the machine block is. */
    @NodeAttribute(name = "mbd2_machine_pos", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class Position extends MachineBlock {
        @OutputPort public BlockPos value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getPos());
        }
    }

    /** The machine block's current block state. */
    @NodeAttribute(name = "mbd2_machine_block_state", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class MachineBlockState extends MachineBlock {
        @OutputPort public BlockState value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getBlockState());
        }
    }

    /** The machine's block entity, for the generic block-entity nodes. */
    @NodeAttribute(name = "mbd2_machine_block_entity", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class Holder extends MachineBlock {
        @OutputPort public BlockEntity value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.asBlockEntity());
        }
    }

    /**
     * Which way the machine faces, or {@code NORTH} for a machine that has no facing.
     *
     * <p>Flattened from the {@code Optional} the machine returns: the graph has no optional type for
     * a {@code Direction}, and every consumer would have to handle the empty case identically anyway.
     * Pair it with {@code Has Front Facing} when the difference matters.</p>
     */
    @NodeAttribute(name = "mbd2_machine_front_facing", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class FrontFacing extends MachineBlock {
        @OutputPort public Direction value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getFrontFacing().orElse(Direction.NORTH));
        }
    }

    /**
     * The world direction one of the machine's own faces currently points in.
     *
     * <p>Everything a player sees is machine-relative — "the left side", "the back" — and everything
     * the machine stores is a world direction resolved against its facing. A UI that lets someone
     * configure a side has to cross that gap, and rotating the machine afterwards has to move the
     * setting with it. This is the conversion, on its own, so the graph can do the crossing
     * explicitly rather than each node guessing which of the two it was handed.</p>
     */
    @NodeAttribute(name = "mbd2_machine_relative_side", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class RelativeSide extends MachineBlock {
        @InputPort public RelativeDirection relative = RelativeDirection.FRONT;
        @OutputPort public Direction value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            var relative = ctx.getInput("relative", RelativeDirection.class, RelativeDirection.FRONT);
            ctx.setOutput("value", relative.getActualFacing(machine.getFrontFacing().orElse(Direction.NORTH)));
        }
    }

    /** Whether this machine has a facing at all. @see FrontFacing */
    @NodeAttribute(name = "mbd2_machine_has_front_facing", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class HasFrontFacing extends MachineBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getFrontFacing().isPresent());
        }
    }

    // ---- identity ----------------------------------------------------------------------------

    /** The machine's definition id, e.g. {@code mymod:crusher}. */
    @NodeAttribute(name = "mbd2_machine_definition_id", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class DefinitionId extends MachineBlock {
        @OutputPort public ResourceLocation value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getDefinition().id());
        }
    }

    /** The machine's definition, for the definition nodes. */
    @NodeAttribute(name = "mbd2_machine_definition", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class Definition extends MachineBlock {
        @OutputPort public MBDMachineDefinition value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getDefinition());
        }
    }

    /** The machine's display name — its custom name if it has one, otherwise its block's. */
    @NodeAttribute(name = "mbd2_machine_name", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class MachineName extends MachineBlock {
        @OutputPort public Component value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getMachineName());
        }
    }

    /** The item this machine drops as. */
    @NodeAttribute(name = "mbd2_machine_drop_item", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class DropItem extends MachineBlock {
        @OutputPort public ItemStack value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getDropItem());
        }
    }

    // ---- state -------------------------------------------------------------------------------

    /** The machine's current state name, as used by the state machine and the renderer. */
    @NodeAttribute(name = "mbd2_machine_state", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class MachineStateName extends MachineBlock {
        @OutputPort public String value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getMachineStateName());
        }
    }

    /** The machine's tier, as configured or dynamically overridden. */
    @NodeAttribute(name = "mbd2_machine_tier", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class MachineTier extends MachineBlock {
        @OutputPort public int value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getMachineLevel());
        }
    }

    /**
     * The machine's free-form persisted tag.
     *
     * <p>Read-only here — the tag is synced and change-tracked, so edits have to go through
     * {@code Set Custom Data} rather than being written into this copy.</p>
     */
    @NodeAttribute(name = "mbd2_machine_custom_data", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class CustomData extends MachineBlock {
        @OutputPort public CompoundTag value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getCustomData());
        }
    }

    /** True on the client. Guard anything that changes world state with this. */
    @NodeAttribute(name = "mbd2_machine_is_remote", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class IsRemote extends MachineBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.isRemote());
        }
    }

    /**
     * A per-machine tick counter, offset so machines at different positions do not all fire on the
     * same tick. The usual driver for staggered periodic work.
     */
    @NodeAttribute(name = "mbd2_machine_offset_timer", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class OffsetTimer extends MachineBlock {
        @OutputPort public long value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getOffsetTimer());
        }
    }

    /**
     * True on one tick in every {@code interval} — the staggered-periodic-work test, done for you.
     *
     * <p>{@link OffsetTimer} into {@code Modulo} into {@code Equals 0} says the same thing, and says
     * it correctly: KilaGraph's arithmetic takes its numeric lane from its operands, so a
     * {@code long} timer stays a {@code long} the whole way through. This block is that chain
     * written once — worth having because reading the clock is the most common thing a blueprint
     * does with a machine, and three nodes and a wire is three places to get the phase or the
     * comparison subtly wrong.</p>
     *
     * <p>The phase is the machine's own, so two machines placed at the same moment still land on
     * different ticks — which is the whole point of {@link OffsetTimer} being offset.</p>
     */
    @NodeAttribute(name = "mbd2_machine_every_n_ticks", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class EveryNTicks extends MachineBlock {
        @InputPort public int interval = 20;
        @OutputPort public boolean value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            var interval = ctx.getInt("interval", 20);
            // <= 0 would be a division by zero, and "every zero ticks" has no useful reading either;
            // false is the answer that makes a mis-set interval do nothing rather than fire always.
            ctx.setOutput("value", interval > 0 && Math.floorMod(machine.getOffsetTimer(), interval) == 0);
        }
    }

    // ---- recipe ------------------------------------------------------------------------------

    /** The machine's recipe logic, for the recipe-logic nodes. */
    @NodeAttribute(name = "mbd2_machine_recipe_logic", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class MachineRecipeLogic extends MachineBlock {
        @OutputPort public RecipeLogic value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getRecipeLogic());
        }
    }

    /** The recipe type this machine runs. */
    @NodeAttribute(name = "mbd2_machine_recipe_type", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(MachineInfoNode.class)
    public static class MachineRecipeType extends MachineBlock {
        @OutputPort public MBDRecipeType value;

        @Override
        protected void read(MBDMachine machine, EvalContext ctx) {
            ctx.setOutput("value", machine.getRecipeType());
        }
    }
}
