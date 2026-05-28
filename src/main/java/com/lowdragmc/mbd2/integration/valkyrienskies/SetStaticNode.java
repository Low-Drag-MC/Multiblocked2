package com.lowdragmc.mbd2.integration.valkyrienskies;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@LDLRegister(name = "set ship static", group = "graph_processor.node.mbd2.machine.valkyrienskies", modID = "valkyrienskies")
public class SetStaticNode extends LinearTriggerNode {
	@InputPort
	public Ship ship;

	@InputPort(name = "is static", tips = {"true = ship is frozen in place.", "get the current ship 'is static' from ship info"})
	public boolean isStatic;

	@Override
	protected void process() {
		if (ship != null) {
			if (ship instanceof ServerShip serverShip) {
                serverShip.setStatic(isStatic);
            }
		}
	}
}
