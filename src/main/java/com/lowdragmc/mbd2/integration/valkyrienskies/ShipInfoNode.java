package com.lowdragmc.mbd2.integration.valkyrienskies;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4dc;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;

@LDLRegister(name = "ship info", group = "graph_processor.node.mbd2.machine.valkyrienskies", modID = "valkyrienskies")
public class ShipInfoNode extends LinearTriggerNode {

	@InputPort
	public Ship ship;

	@OutputPort
	public String slug;

	@OutputPort
	public Number id;

	@OutputPort(tips = "linear velocity of the ship, in metres per second")
	public Vector3f velocity;

    @OutputPort(tips = "rotational velocity of the ship, in radians per second", name = "angular velocity")
    public Vector3f angularVelocity;

    @OutputPort(tips = "weight of the ship, in kg. weight is null on client")
    public Number mass;

    @OutputPort(tips = "is the ship static. always false on client", name = "is static")
    public boolean isStatic;

    @OutputPort(tips = "the xyz of the center of mass in ship space. null on client", name = "center of mass xyz")
    public Vector3f centerOfMass;

    @OutputPort(tips = {"whether this ship variable is server-side", "handy for making sure you don't get the client defaults", "for isStatic and other values"}, name = "is server")
    public boolean isServer;

	@OutputPort(name = "ship to world transform")
	public Matrix4dc shipToWorld;

	@OutputPort(name = "world to ship transform")
	public Matrix4dc worldToShip;

	@Override
	protected void process() {
		if (ship != null) {
			slug = ship.getSlug();
			id = ship.getId();

            isStatic = false;
            isServer = false;

            velocity = ship.getVelocity().get(new Vector3f());
            angularVelocity = ship.getAngularVelocity().get(new Vector3f());

			worldToShip = ship.getTransform().getWorldToShip();
			shipToWorld = ship.getTransform().getShipToWorld();

            if (ship instanceof ServerShip serverShip) {
                isServer = true;
                mass = serverShip.getInertiaData().getMass();
                isStatic = serverShip.isStatic();
                centerOfMass = serverShip.getInertiaData().getCenterOfMass().get(new Vector3f());
            }
		}
	}
}
