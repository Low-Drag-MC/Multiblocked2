package com.lowdragmc.mbd2.integration.valkyrienskies;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@LDLRegister(name = "get ship", group = "graph_processor.node.mbd2.machine.valkyrienskies", modID = "valkyrienskies")
public class GetShipNode extends LinearTriggerNode {
	@InputPort
	public Level level;
	@InputPort
	public Vector3f xyz;
	@OutputPort
	public Ship ship;

	@Override
	protected void process() {
		if (xyz != null && level != null) {
			ship = VSGameUtilsKt.getShipManagingPos(level, new BlockPos((int) xyz.x, (int) xyz.y, (int) xyz.z));
		}
	}
}
