package com.lowdragmc.mbd2.common.blueprint.node.ui;

import com.lowdragmc.kilagraph.blueprint.nodes.ui.UIActions;
import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.ExecInputPort;
import com.lowdragmc.kilagraph.graph.core.ExecOutputPort;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles.ExecutionFlow;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import net.minecraft.world.item.ItemStack;

/**
 * Drawing an item on a UI element from a graph.
 */
public final class UIItemNodes {

    private static final String GROUP = "mbd2/ui";

    private UIItemNodes() {}

    /**
     * Draws an item stack on an element.
     *
     * <p>{@code Lss Set} covers every other texture, because every other texture can be written as a
     * string — {@code icon(...)}, {@code rect(...)}, a colour. An item cannot: it is a live
     * {@link ItemStack} with components, and the lss grammar has no function for one. So the graph
     * needs a way to hand an element an object rather than a string, and this is it.</p>
     *
     * <p>Not an {@code ItemSlot}: a slot is an element, and an element inside a clickable one becomes
     * the click's target, which silently stops the parent's listener from ever running. A texture is
     * painted by the element that already has the listener, so there is nothing new to click.</p>
     *
     * <p>It goes on the element's <em>overlay</em>, and fills it — how big the item is drawn is the
     * element's own size, which is a stylesheet's business rather than this node's.</p>
     *
     * <p>An empty stack clears the overlay rather than drawing an empty item, so an element with
     * nothing to show is left plain instead of holding a stale one.</p>
     */
    @NodeAttribute(name = "mbd2_ui_set_item", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetItem extends AnnotatedNode {
        @ExecInputPort public ExecutionFlow trigger;
        @ExecOutputPort public ExecutionFlow next;

        @InputPort public UIElement element;
        @InputPort public ItemStack item = ItemStack.EMPTY;
        @OutputPort public UIElement out;
        @OutputPort public boolean ok;

        @Override
        public void execute(ExecContext ctx) {
            var element = UIActions.element(ctx, "element");
            ctx.setOutput("out", element);
            if (element == null) {
                UIActions.done(ctx, false);
                return;
            }
            var texture = texture(ctx.getInput("item", ItemStack.class, ItemStack.EMPTY));
            // style(..) is a no-op on a dedicated server by design, so this needs no side guard: the
            // server half of the tree keeps the same shape and simply paints nothing.
            element.style(style -> style.overlay(texture));
            UIActions.done(ctx, true);
        }

        @Override
        public void evaluate(EvalContext ctx) {
            ctx.setOutput("out", UIActions.element(ctx, "element"));
        }

        private static IGuiTexture texture(ItemStack stack) {
            return stack == null || stack.isEmpty() ? IGuiTexture.EMPTY : new ItemStackTexture(stack);
        }
    }
}
