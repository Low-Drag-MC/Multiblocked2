package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineTargetActionNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * The Photon effect actions a blueprint can take on a machine.
 *
 * <p>All {@code modID = "photon"}: {@code GraphNodeRegistry} filters on the annotation <em>scan
 * data</em>, so without Photon these classes are never loaded, never registered, and never offered in
 * the node palette. They still name no Photon type themselves — everything goes through
 * {@link MBDMachine}'s RPC methods — so nothing here depends on that filter for safety, only for
 * keeping the palette honest.</p>
 *
 * <p>{@code Side.BOTH} for the same reason as {@code MachineActionNodes.TriggerAnimation}: the
 * machine relays to tracking clients itself when called on the server and plays locally when called
 * on the client, so both a server-side {@code State Changed} and a {@code Client Tick} reach the
 * effect correctly.</p>
 *
 * <p>Note what these are <em>not</em> for: an effect that should be visible to a player who arrives
 * later, or that must survive a relog, belongs on a machine state — an RPC only reaches whoever is
 * tracking the chunk at the moment it fires. See {@code MBDMachine.syncStateFX}.</p>
 */
public final class PhotonFXNodes {

    private static final String GROUP = "mbd2/machine/fx";

    private PhotonFXNodes() {}

    /**
     * Play one of the machine's named effects, as authored in its Machine FX list.
     *
     * <p>Idempotent unless the entry sets {@code replace existing}: firing this every tick while the
     * effect is already running does nothing.</p>
     */
    @NodeAttribute(name = "mbd2_machine_play_fx", group = GROUP, modID = "photon",
            graphTypes = MachineBlueprintGraph.class)
    public static class PlayFX extends MachineTargetActionNode {
        @InputPort public String name = "";

        @Override
        protected Side side() {
            return Side.BOTH;
        }

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var name = ctx.getInput("name", String.class, "");
            if (!name.isEmpty()) machine.playMachineFX(name);
        }
    }

    /**
     * Stop a named effect. Unknown names do nothing.
     *
     * <p>{@code forcedDeath} drops the particles still on screen immediately; leaving it off lets
     * them finish their own lifetime, which is what you want for smoke that should trail off.</p>
     */
    @NodeAttribute(name = "mbd2_machine_stop_fx", group = GROUP, modID = "photon",
            graphTypes = MachineBlueprintGraph.class)
    public static class StopFX extends MachineTargetActionNode {
        @InputPort public String name = "";
        @InputPort public boolean forcedDeath;

        @Override
        protected Side side() {
            return Side.BOTH;
        }

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var name = ctx.getInput("name", String.class, "");
            if (!name.isEmpty()) {
                machine.stopMachineFX(name, ctx.getInput("forcedDeath", Boolean.class, false));
            }
        }
    }

    /**
     * Play an effect described inline, for a blueprint that computes its effect rather than picking
     * one. Prefer {@link PlayFX} otherwise — a library entry is authorable and previewable in the
     * editor, and needs no pins.
     *
     * <p>{@code identifier} is the slot the effect occupies: starting another one under the same
     * identifier either replaces it or is refused, per {@code replaceExisting}.</p>
     */
    @NodeAttribute(name = "mbd2_machine_emit_fx", group = GROUP, modID = "photon",
            graphTypes = MachineBlueprintGraph.class)
    public static class EmitFX extends MachineTargetActionNode {
        @InputPort public String identifier = "";
        @InputPort public String fxLocation = "";
        @InputPort public Vector3f offset;
        @InputPort public Vector3f rotation;
        @InputPort public Vector3f scale = new Vector3f(1, 1, 1);
        @InputPort public int delay;
        @InputPort public boolean forcedDeath;
        @InputPort public boolean replaceExisting;

        @Override
        protected Side side() {
            return Side.BOTH;
        }

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var identifier = ctx.getInput("identifier", String.class, "");
            var location = ResourceLocation.tryParse(ctx.getInput("fxLocation", String.class, ""));
            if (identifier.isEmpty() || location == null) return;
            machine.emitPhotonFx(identifier, location,
                    ctx.getInput("offset", Vector3f.class, null),
                    ctx.getInput("rotation", Vector3f.class, null),
                    ctx.getInput("scale", Vector3f.class, null),
                    ctx.getInput("delay", Integer.class, 0),
                    ctx.getInput("forcedDeath", Boolean.class, false),
                    ctx.getInput("replaceExisting", Boolean.class, false));
        }
    }

    /**
     * Kill an effect by identifier, whatever started it.
     *
     * <p>The same operation as {@link StopFX} — both stop one slot — kept as its own node so
     * {@code Emit Photon FX} has a matching partner in the palette, the way {@code Play}/{@code Stop
     * Machine FX} do. Named effects and ad-hoc ones share a slot namespace, so either node reaches
     * either kind.</p>
     *
     * @see StopFX for {@code forcedDeath}
     */
    @NodeAttribute(name = "mbd2_machine_kill_fx", group = GROUP, modID = "photon",
            graphTypes = MachineBlueprintGraph.class)
    public static class KillFX extends MachineTargetActionNode {
        @InputPort public String identifier = "";
        @InputPort public boolean forcedDeath;

        @Override
        protected Side side() {
            return Side.BOTH;
        }

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var identifier = ctx.getInput("identifier", String.class, "");
            if (!identifier.isEmpty()) {
                machine.stopMachineFX(identifier, ctx.getInput("forcedDeath", Boolean.class, false));
            }
        }
    }
}
