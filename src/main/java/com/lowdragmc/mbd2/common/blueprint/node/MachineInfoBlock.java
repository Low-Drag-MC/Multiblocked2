package com.lowdragmc.mbd2.common.blueprint.node;

import com.lowdragmc.kilagraph.graph.core.AnnotatedBlockNode;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import org.jetbrains.annotations.Nullable;

/**
 * Base for a block reading one property of its parent context node's target.
 *
 * <p>The same idea as KilaGraph's {@code InfoPropertyBlock}, and re-declared here for one reason: the
 * fallback. A machine blueprint almost always reads its own machine, so a context whose {@code target}
 * is unwired resolves to the blueprint's machine instead of to null. KilaGraph's version has no notion
 * of a host-supplied default and its target lookup is private, so there is nothing to extend.</p>
 *
 * <p>Missing target → {@link #read} is not called and every output goes unstaged, which the executor
 * publishes as null and a consumer sees as its own declared default. Throwing would make one unwired
 * context break the whole evaluation rather than the branch that depended on it.</p>
 *
 * @param <T> the target type this block knows how to read
 */
public abstract class MachineInfoBlock<T> extends AnnotatedBlockNode {

    /** The type this block reads. Checked against the parent's target before {@link #read}. */
    protected abstract Class<T> targetClass();

    /** Write this block's outputs from {@code target}. Only called with a non-null, correctly typed one. */
    protected abstract void read(T target, EvalContext ctx);

    @Override
    public final void evaluate(EvalContext ctx) {
        Object target = parentTarget(ctx);
        if (targetClass().isInstance(target)) {
            read(targetClass().cast(target), ctx);
        }
    }

    /**
     * The parent context's {@code target}, falling back to whatever the context derives from the
     * blueprint's own machine when nothing is wired.
     */
    @Nullable
    private Object parentTarget(EvalContext ctx) {
        var block = getBlockNodeModel();
        if (block == null) return null;
        var contextModel = block.getContextNodeModel();
        if (contextModel == null) return null;
        var port = contextModel.getInputsById().get("target");
        Object wired = port == null ? null : ctx.getExecutor().pullInputValue(port);
        if (wired != null) return wired;
        return contextModel instanceof ICustomNodeModel custom
                && custom.getNode() instanceof MachineInfoContextNode<?> context
                ? context.defaultTarget(ctx)
                : null;
    }
}
