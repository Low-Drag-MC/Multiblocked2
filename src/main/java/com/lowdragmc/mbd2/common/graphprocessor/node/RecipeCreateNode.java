package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

@LDLRegister(name = "recipe create", group = "graph_processor.node.mbd2.machine.recipe")
public class RecipeCreateNode extends BaseNode {
    @InputPort
    public Object in;
    @InputPort
    public String id;
    @OutputPort
    public MBDRecipe out;
    @Configurable(name = "id")
    public String internalID = "mbd2:recipe_on_the_fly";

    @Override
    protected void process() {
        if (id != null && !ResourceLocation.isValidResourceLocation(id)) return;
        var recipeID = new ResourceLocation(id == null ? internalID : id);
        if (in instanceof CompoundTag tag) {
            out = MBDRecipeSerializer.SERIALIZER.fromNBT(recipeID, tag);
        } else if (in instanceof JsonObject json) {
            out = MBDRecipeSerializer.SERIALIZER.fromJson(recipeID, json);
        } else if (in instanceof CharSequence) {
            var json = JsonParser.parseString(in.toString());
            if (json.isJsonObject()) {
                out = MBDRecipeSerializer.SERIALIZER.fromJson(recipeID, json.getAsJsonObject());
            }
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
