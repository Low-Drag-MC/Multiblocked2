package com.lowdragmc.mbd2.integration.valkyrienskies;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;

import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3f;

@LDLRegister(name = "transform direction", group = "graph_processor.node.mbd2.machine.valkyrienskies", modID = "valkyrienskies")
public class TransformRotNode extends LinearTriggerNode {
	@InputPort(tips = "ship transform to use, get from ship info")
	public Matrix4dc transform;

	@InputPort(name = "direction xyz")
	public Vector3f xyz;

	@OutputPort(name = "transformed dir xyz")
	public Vector3f new_xyz;

	@Override
	protected void process() {
		if (transform != null && xyz != null) {
			Vector3d newRot = transform.transformDirection(new Vector3d(xyz), new Vector3d());
			new_xyz = newRot.get(new Vector3f());
		}
	}
}
