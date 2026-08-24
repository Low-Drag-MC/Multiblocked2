package com.lowdragmc.mbd2.common.blueprint.node;

import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.type.KGTypeHandles;
import com.lowdragmc.kilagraph.graph.util.INodeDescription;
import com.lowdragmc.kilagraph.graph.util.NodeDescriptions;
import com.lowdragmc.kilagraph.graph.util.NodeTooltipHelper;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.ContextNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A context node holding one {@code target} whose properties the {@link MachineInfoBlock}s inside it
 * read.
 *
 * <p>The shape is KilaGraph's: wire the target once, then stack as many property blocks inside as the
 * graph needs, rather than running the same wire into a node per property. Each property is a dedicated
 * class with typed outputs rather than a name-driven reflective read — see
 * {@code InfoContextNode}'s javadoc for why that trade is worth one class per property.</p>
 *
 * <p>What is added here is {@link #defaultTarget}: with nothing wired, the context resolves from the
 * blueprint's own machine. Reading your own machine is the overwhelmingly common case, and it should
 * not cost a wire.</p>
 *
 * @param <T> the type whose properties the contained blocks read
 */
public abstract class MachineInfoContextNode<T> extends ContextNode implements INodeDescription {

    /** The class whose properties this context's blocks read. Drives the {@code target} port. */
    protected abstract Class<T> targetClass();

    /**
     * What the context reads when {@code target} is unwired — derived from the blueprint's own machine.
     * Return {@code null} for a context that has no sensible default.
     */
    @Nullable
    protected abstract T defaultTarget(EvalContext ctx);

    @Override
    public void setImplementation(NodeModel nodeModel) {
        super.setImplementation(nodeModel);
        NodeTooltipHelper.apply(nodeModel, getNodeTooltip());
    }

    protected @Nullable Component getNodeTooltip() {
        return NodeTooltipHelper.defaultTooltip(this);
    }

    @Override
    @Nullable
    public UIElement createDescriptionUI() {
        return NodeDescriptions.build(this);
    }

    @Override
    public Component getDisplayName() {
        var attribute = getClass().getAnnotation(NodeAttribute.class);
        return attribute == null
                ? Component.literal(targetClass().getSimpleName() + " Info")
                : Component.translatable(attribute.name());
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        // Every target here is a live object with no AccessorRegistries entry, so there is no inline
        // editor and no embedded constant to build — same handling as KilaGraph's annotated ports.
        context.addInputPort("target", KGTypeHandles.handleFor(targetClass())).withoutConfigurator();
    }
}
