package com.lowdragmc.mbd2.integration.valkyrienskies;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;

import net.minecraft.core.BlockPos;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;

@LDLRegister(name = "apply force to ship", group = "graph_processor.node.mbd2.machine.valkyrienskies", modID = "valkyrienskies")
public class ApplyForceNode extends LinearTriggerNode {
    public enum Space {
        BODY,
        MODEL,
        WORLD;
    }

	@InputPort
	public Ship ship;

	@InputPort(name = "direction xyz", tips = {"if you're getting this from a block Direction, you might ", "want to use transform rotation on it first (with shipToWorld)"})
	public Vector3f dir_xyz;

	@InputPort(tips = "force multiplier, e.g. strength")
	public int force;

	@InputPort(name = "location based", tips = "is force applied at the 'pos xyz', else the force will be global")
	public boolean location_based = true;

    @InputPort(name = "pos xyz")
    public Vector3f pos_xyz;

    @InputPort(
            name = "offset xyz",
            tips = {
                "due to floating point precision, it can be difficult to add a small offset to your shipyard positions.",
                "this value will be added to your xyz input before applying the force at that position. ",
                "This is useful to apply forces at the center of a block, where a 0.5 offset is necessary."
            }
    )
    public Vector3f offset_xyz;

    @Configurable(name = "coordinate space", tips = {"BODY = offset from COM", "MODEL = shipyard block position", "WORLD = world block position, e.g. nearby the ship"})
    public Space coordinateSpace = Space.MODEL;

	@Override
	protected void process() {
		if (ship != null && pos_xyz != null && force != 0) {

			GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(ship.getChunkClaimDimension());
            Vector3d dir = new Vector3d(dir_xyz.x, dir_xyz.y, dir_xyz.z);

            long id = ship.getId();
            Vector3d forceVec = dir.mul(force);
            Vector3d posVec = new Vector3d(pos_xyz);
            if (offset_xyz != null) {
                posVec.add(offset_xyz);
            }

            if (location_based) {
                switch (coordinateSpace) {
                    case BODY -> gtpa.applyWorldForceToBodyPos(id, forceVec, posVec);
                    case MODEL -> gtpa.applyWorldForceToModelPos(id, forceVec, posVec);
                    case WORLD -> gtpa.applyWorldForce(id, forceVec, posVec);
                }
            } else {
                gtpa.applyInvariantForce(ship.getId(), dir.mul(force));
            }
        }
	}
}
