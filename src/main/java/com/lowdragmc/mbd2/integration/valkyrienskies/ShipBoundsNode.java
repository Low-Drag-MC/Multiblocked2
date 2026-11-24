package com.lowdragmc.mbd2.integration.valkyrienskies;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;

@LDLRegister(name = "ship bounds", group = "graph_processor.node.mbd2.machine.valkyrienskies", modID = "valkyrienskies")
public class ShipBoundsNode extends LinearTriggerNode {
	@InputPort
	public Ship ship;

	@OutputPort(tips = "minimum xyz of the ships bounds in ship space", name = "ship from")
	public Vector3f ship_from;
    @OutputPort(tips = "maximum xyz of the ships bounds in ship space", name = "ship to")
	public Vector3f ship_to;

    @OutputPort(tips = "minimum xyz of the ships bounds in world space", name = "world from")
    public Vector3f world_from;
    @OutputPort(tips = "maximum xyz of the ships bounds in world space", name = "world to")
    public Vector3f world_to;

	@Override
	protected void process() {
		if (ship != null) {
            AABBic shipAABB = ship.getShipAABB();
            if (shipAABB != null) {
                ship_from = new Vector3f(shipAABB.minX(), shipAABB.minY(), shipAABB.minZ());
                ship_to = new Vector3f(shipAABB.maxX(), shipAABB.maxY(), shipAABB.maxZ());
            }
            AABBdc worldAABB = ship.getWorldAABB();
            world_from = new Vector3d(worldAABB.minX(), worldAABB.minY(), worldAABB.minZ()).get(new Vector3f());
            world_to = new Vector3d(worldAABB.maxX(), worldAABB.maxY(), worldAABB.maxZ()).get(new Vector3f());
        }
	}
}
