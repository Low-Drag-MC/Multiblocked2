package com.lowdragmc.mbd2.common.blueprint.node.trait;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import org.jetbrains.annotations.Nullable;

/**
 * Base for "this trait, as capability {@code T}" — the one bridge every capability node set needs.
 *
 * <p>Subclasses declare the two output ports ({@code value} and {@code found}) and name the capability
 * class; everything else — resolving the trait, picking the IO face, the type check — is here. Written
 * once rather than per mod, so a Mekanism chemical tank and a vanilla item slot are reached exactly the
 * same way.</p>
 *
 * <p>The two ways to name a trait both exist because both are natural: a graph touching one trait many
 * times resolves it once with {@code Machine Trait} and fans the wire out, while a graph touching it
 * once puts the name here and skips a node.</p>
 *
 * @param <T> the capability interface this node produces
 */
public abstract class TraitCapabilityNode<T> extends AnnotatedNode {

    @InputPort
    public ITrait trait;
    @InputPort
    public String traitName = "";
    @InputPort
    public IO io = IO.BOTH;

    /** The capability interface to narrow the trait's contents to. */
    protected abstract Class<T> capabilityClass();

    @Override
    public final void evaluate(EvalContext ctx) {
        T value = resolve(ctx);
        ctx.setOutput("value", value);
        ctx.setOutput("found", value != null);
    }

    @Nullable
    private T resolve(EvalContext ctx) {
        var trait = resolveTrait(ctx);
        if (!(trait instanceof SimpleCapabilityTrait<?, ?> capability)) return null;
        Object content = capability.getCapContent(ctx.getInput("io", IO.class, IO.BOTH));
        return capabilityClass().isInstance(content) ? capabilityClass().cast(content) : null;
    }

    @Nullable
    private ITrait resolveTrait(EvalContext ctx) {
        var explicit = ctx.getInput("trait", ITrait.class, null);
        if (explicit != null) return explicit;
        var name = ctx.getInput("traitName", String.class, "");
        if (name.isEmpty()) return null;
        var machine = MachineNodes.ownMachine(ctx.getExecutor());
        return machine == null ? null : machine.getTraitByName(name);
    }
}
