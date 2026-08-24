package com.lowdragmc.mbd2.common.blueprint.node.trait;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Moving items in and out of an {@code IItemHandler}.
 *
 * <p>KilaGraph already reads containers — size, slot contents, counting, finding — so only the mutating
 * half lives here. That split is deliberate: a read is a pull-based data node, while an insert is an
 * action that must happen once, in order, on the exec path. Wiring an insert as a data node would let
 * the executor's memo table decide how many times it ran.</p>
 *
 * <p>Every mutation reports what it actually moved, so a graph can react to a partial transfer instead
 * of assuming success. {@code simulate} runs the same arithmetic and changes nothing, which is how you
 * ask "would this fit?" before committing.</p>
 */
public final class ItemHandlerNodes {

    private static final String GROUP = "mbd2/trait/item";

    private ItemHandlerNodes() {}

    /**
     * Insert a stack, trying every slot.
     *
     * <p>{@code remainder} is what did not fit — empty on a full insert. That is the value to feed
     * onward, not the original stack.</p>
     */
    @NodeAttribute(name = "mbd2_item_insert", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Insert extends MachineActionNode {
        @InputPort public IItemHandler handler;
        @InputPort public ItemStack stack = ItemStack.EMPTY;
        @InputPort public boolean simulate = false;
        @OutputPort public ItemStack remainder;
        @OutputPort public int inserted;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IItemHandler.class, null);
            var stack = ctx.getInput("stack", ItemStack.class, ItemStack.EMPTY);
            if (handler == null || stack == null || stack.isEmpty()) {
                ctx.setOutput("remainder", stack == null ? ItemStack.EMPTY : stack);
                ctx.setOutput("inserted", 0);
                return;
            }
            boolean simulate = ctx.getInput("simulate", Boolean.class, false);
            ItemStack remaining = stack.copy();
            for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = handler.insertItem(slot, remaining, simulate);
            }
            ctx.setOutput("remainder", remaining);
            ctx.setOutput("inserted", stack.getCount() - remaining.getCount());
        }
    }

    /** Insert into one specific slot. @see Insert */
    @NodeAttribute(name = "mbd2_item_insert_slot", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class InsertSlot extends MachineActionNode {
        @InputPort public IItemHandler handler;
        @InputPort public int slot;
        @InputPort public ItemStack stack = ItemStack.EMPTY;
        @InputPort public boolean simulate = false;
        @OutputPort public ItemStack remainder;
        @OutputPort public int inserted;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IItemHandler.class, null);
            var stack = ctx.getInput("stack", ItemStack.class, ItemStack.EMPTY);
            int slot = ctx.getInput("slot", Integer.class, 0);
            if (handler == null || stack == null || stack.isEmpty() || slot < 0 || slot >= handler.getSlots()) {
                ctx.setOutput("remainder", stack == null ? ItemStack.EMPTY : stack);
                ctx.setOutput("inserted", 0);
                return;
            }
            var remainder = handler.insertItem(slot, stack, ctx.getInput("simulate", Boolean.class, false));
            ctx.setOutput("remainder", remainder);
            ctx.setOutput("inserted", stack.getCount() - remainder.getCount());
        }
    }

    /** Take up to {@code amount} items out of one slot. */
    @NodeAttribute(name = "mbd2_item_extract_slot", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ExtractSlot extends MachineActionNode {
        @InputPort public IItemHandler handler;
        @InputPort public int slot;
        @InputPort public int amount = 1;
        @InputPort public boolean simulate = false;
        @OutputPort public ItemStack extracted;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IItemHandler.class, null);
            int slot = ctx.getInput("slot", Integer.class, 0);
            int amount = ctx.getInput("amount", Integer.class, 1);
            if (handler == null || slot < 0 || slot >= handler.getSlots() || amount <= 0) {
                ctx.setOutput("extracted", ItemStack.EMPTY);
                return;
            }
            ctx.setOutput("extracted",
                    handler.extractItem(slot, amount, ctx.getInput("simulate", Boolean.class, false)));
        }
    }

    /**
     * Overwrite a slot outright.
     *
     * <p>Only works on a handler that admits it — MBD2's own item traits do; an arbitrary modded
     * container may not. {@code applied} says which happened, so a graph can fall back to
     * insert/extract rather than silently losing the write.</p>
     */
    @NodeAttribute(name = "mbd2_item_set_slot", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetSlot extends MachineActionNode {
        @InputPort public IItemHandler handler;
        @InputPort public int slot;
        @InputPort public ItemStack stack = ItemStack.EMPTY;
        @OutputPort public boolean applied;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IItemHandler.class, null);
            int slot = ctx.getInput("slot", Integer.class, 0);
            var stack = ctx.getInput("stack", ItemStack.class, ItemStack.EMPTY);
            if (!(handler instanceof IItemHandlerModifiable modifiable)
                    || slot < 0 || slot >= handler.getSlots() || stack == null) {
                ctx.setOutput("applied", false);
                return;
            }
            modifiable.setStackInSlot(slot, stack.copy());
            ctx.setOutput("applied", true);
        }
    }

    /** Whether a slot would accept a stack — its filter, not whether there is room. */
    @NodeAttribute(name = "mbd2_item_is_valid", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class IsValid extends AnnotatedNode {
        @InputPort public IItemHandler handler;
        @InputPort public int slot;
        @InputPort public ItemStack stack = ItemStack.EMPTY;
        @OutputPort public boolean value;

        @Override
        public void evaluate(EvalContext ctx) {
            var handler = ctx.getInput("handler", IItemHandler.class, null);
            int slot = ctx.getInput("slot", Integer.class, 0);
            var stack = ctx.getInput("stack", ItemStack.class, ItemStack.EMPTY);
            ctx.setOutput("value", handler != null && stack != null
                    && slot >= 0 && slot < handler.getSlots()
                    && handler.isItemValid(slot, stack));
        }
    }

    /** How many items a slot can hold — its own limit, which may be below the item's max stack size. */
    @NodeAttribute(name = "mbd2_item_slot_limit", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SlotLimit extends AnnotatedNode {
        @InputPort public IItemHandler handler;
        @InputPort public int slot;
        @OutputPort public int value;

        @Override
        public void evaluate(EvalContext ctx) {
            var handler = ctx.getInput("handler", IItemHandler.class, null);
            int slot = ctx.getInput("slot", Integer.class, 0);
            ctx.setOutput("value", handler == null || slot < 0 || slot >= handler.getSlots()
                    ? 0 : handler.getSlotLimit(slot));
        }
    }
}
