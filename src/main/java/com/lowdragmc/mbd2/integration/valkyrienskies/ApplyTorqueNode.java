package com.lowdragmc.mbd2.integration.valkyrienskies;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;

@LDLRegister(name = "apply torque to ship", group = "graph_processor.node.mbd2.machine.valkyrienskies", modID = "valkyrienskies")
public class ApplyTorqueNode extends LinearTriggerNode {
    public enum Space {
        BODY,
        MODEL,
        WORLD;
    }

	@InputPort
	public Ship ship;

	@InputPort(name = "torque xyz", tips = {"if you're getting this from a block Direction, you might ", "want to use transform rotation on it first (with shipToWorld)"})
	public Vector3f torque_xyz;

	@InputPort(tips = "torque multiplier, aka strength")
	public int strength;

    @Configurable(name = "coordinate space", tips = {"BODY = relative to COM", "MODEL = relative to shipyard", "WORLD = relative to world"})
    public Space coordinateSpace = Space.MODEL;

	@Override
	protected void process() {
		if (ship != null && torque_xyz != null && strength != 0) {

			GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(ship.getChunkClaimDimension());
            Vector3d torque = new Vector3d(torque_xyz.x, torque_xyz.y, torque_xyz.z);

            long id = ship.getId();
            Vector3d forceVec = torque.mul(strength);

            switch (coordinateSpace) {
                case BODY -> gtpa.applyBodyTorque(id, forceVec);
                case MODEL -> gtpa.applyModelTorque(id, forceVec);
                case WORLD -> gtpa.applyWorldTorque(id, forceVec);
            }

        }
	}
}
