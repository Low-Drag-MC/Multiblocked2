package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

@LDLRegister(name = "recipe info", group = "graph_processor.node.mbd2.machine.recipe")
public class RecipeInfoNode extends BaseNode {
    @InputPort
    public MBDRecipe recipe;
    @OutputPort(name = "recipe id")
    public String recipeID;
    @OutputPort
    public int duration;
    @OutputPort
    public int priority;
    @OutputPort(name = "is fuel recipe")
    public boolean isFuel;
    @OutputPort(name = "data")
    public CompoundTag data;

    @Override
    protected void process() {
        if (recipe != null) {
            recipeID = recipe.getId().toString();
            duration = recipe.duration;
            priority = recipe.priority;
            isFuel = recipe.isFuel;
            data = recipe.data;
        }
    }
}
