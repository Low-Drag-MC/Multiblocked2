package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

@LDLRegister(name = "recipe deserialize", group = "graph_processor.node.mbd2.machine.recipe")
public class RecipeDeserializeNode extends BaseNode {
    @InputPort
    public MBDRecipe in;
    @OutputPort
    public String id;
    @OutputPort
    public CompoundTag recipe;

    @Override
    protected void process() {
        id = null;
        recipe = null;
        if (in != null) {
            id = in.getId().toString();
            recipe = (CompoundTag) MBDRecipeSerializer.CODEC.codec().encodeStart(NbtOps.INSTANCE, in).getOrThrow();
        }
    }
}
