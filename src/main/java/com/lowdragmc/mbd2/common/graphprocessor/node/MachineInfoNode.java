package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

@LDLRegister(name = "machine info", group = "graph_processor.node.mbd2.machine")
public class MachineInfoNode extends BaseNode {
    @InputPort
    public MBDMachine machine;
    @OutputPort
    public Level level;
    @OutputPort
    public Vector3f xyz;
    @OutputPort
    public Direction front;
    @OutputPort
    public String status;
    @OutputPort(name = "recipe status", tips = "graph_processor.node.mbd2.recipe_logic.status.tips")
    public String recipeStatus;
    @OutputPort(name = "custom data")
    public CompoundTag customData;

    @Override
    protected void process() {
        if (machine != null) {
            var pos = machine.getPos();
            level = machine.getLevel();
            xyz = new Vector3f(pos.getX(), pos.getY(), pos.getZ());
            front = machine.getFrontFacing().orElse(Direction.NORTH);
            status = machine.getMachineState().name();
            recipeStatus = machine.getRecipeLogic().getStatus().toString();
            customData = machine.getCustomData();
        }
    }
}
