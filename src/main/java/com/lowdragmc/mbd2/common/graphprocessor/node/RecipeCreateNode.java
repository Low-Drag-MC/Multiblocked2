package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;

@LDLRegister(name = "recipe create", group = "graph_processor.node.mbd2.machine.recipe")
public class RecipeCreateNode extends BaseNode {
    @InputPort
    public Object in;
    @InputPort
    public String id;
    @OutputPort
    public MBDRecipe out;

    @Override
    protected void process() {
        if (id != null && !ResourceLocation.isValidPath(id)) return;
        DataResult<MBDRecipe> result;
        if (in instanceof CompoundTag tag) {
            result = MBDRecipeSerializer.CODEC.codec().parse(NbtOps.INSTANCE, tag);
        } else if (in instanceof JsonElement json) {
            result = MBDRecipeSerializer.CODEC.codec().parse(JavaOps.INSTANCE, json);
        } else if (in instanceof CharSequence) {
            result = MBDRecipeSerializer.CODEC.codec().parse(JavaOps.INSTANCE, JsonParser.parseString(in.toString()));
        } else {
            result = DataResult.error(() -> "Invalid input type");
        }
        if (result.isError()) {
            out = null;
        } else {
            out = result.getOrThrow();
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("id")) {
                if (!port.getEdges().isEmpty()) return;
            }
        }
        super.buildConfigurator(father);
    }
}
