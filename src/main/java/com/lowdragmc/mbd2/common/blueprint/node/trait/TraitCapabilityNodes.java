package com.lowdragmc.mbd2.common.blueprint.node.trait;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The bridge from a machine's traits to the plain capability interfaces the rest of the graph speaks.
 *
 * <h2>Why a bridge rather than a parallel node set</h2>
 * KilaGraph already carries {@code IItemHandler} and {@code IFluidHandler} as wire types, with nodes to
 * read and drain them, and every pipe and hopper in the game speaks the same two interfaces. So the only
 * thing MBD2 has to add is "which handler is this trait" — once a trait is an {@code IItemHandler}, the
 * generic container nodes work on it unchanged, and a graph that operates on a machine's slots is the
 * same graph that operates on a chest's.
 *
 * <p>The {@code IO} input picks which face of the trait to look at, because a trait can expose different
 * contents for insertion and extraction. {@code BOTH} is the unrestricted view and is what you want
 * unless you are deliberately respecting the machine's own IO configuration.</p>
 */
public final class TraitCapabilityNodes {

    private static final String GROUP = "mbd2/trait";

    private TraitCapabilityNodes() {}

    /** A trait's item slots, as the {@code IItemHandler} every container node speaks. */
    @NodeAttribute(name = "mbd2_trait_item_handler", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ItemHandlerOf extends TraitCapabilityNode<IItemHandler> {
        @OutputPort public IItemHandler value;
        @OutputPort public boolean found;

        @Override
        protected Class<IItemHandler> capabilityClass() {
            return IItemHandler.class;
        }
    }

    /** A trait's fluid tanks, as an {@code IFluidHandler}. */
    @NodeAttribute(name = "mbd2_trait_fluid_handler", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class FluidHandlerOf extends TraitCapabilityNode<IFluidHandler> {
        @OutputPort public IFluidHandler value;
        @OutputPort public boolean found;

        @Override
        protected Class<IFluidHandler> capabilityClass() {
            return IFluidHandler.class;
        }
    }

    /** A trait's energy buffer, as an {@code IEnergyStorage}. */
    @NodeAttribute(name = "mbd2_trait_energy_storage", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class EnergyStorageOf extends TraitCapabilityNode<IEnergyStorage> {
        @OutputPort public IEnergyStorage value;
        @OutputPort public boolean found;

        @Override
        protected Class<IEnergyStorage> capabilityClass() {
            return IEnergyStorage.class;
        }
    }

    /** A trait's definition — its name and the settings the machine editor configured. */
    @NodeAttribute(name = "mbd2_trait_definition", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Definition extends AnnotatedNode {
        @InputPort public ITrait trait;
        @OutputPort public TraitDefinition definition;
        @OutputPort public String name;

        @Override
        public void evaluate(EvalContext ctx) {
            var trait = ctx.getInput("trait", ITrait.class, null);
            var definition = trait == null ? null : trait.getDefinition();
            ctx.setOutput("definition", definition);
            ctx.setOutput("name", definition == null ? "" : definition.getName());
        }
    }
}
