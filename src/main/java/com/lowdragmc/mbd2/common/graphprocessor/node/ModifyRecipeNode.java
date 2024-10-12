package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;

import java.util.HashMap;

@LDLRegister(name = "modify recipe", group = "graph_processor.node.mbd2.machine.recipe")
public class ModifyRecipeNode extends BaseNode {
    @InputPort
    public MBDRecipe in;
    @InputPort(name = "content modifier")
    public ContentModifier contentModifier;
    @InputPort(name = "content side")
    public IO contentIO;
    @InputPort(name = "duration modifier")
    public ContentModifier durationModifier;

    @OutputPort
    public MBDRecipe out;

    @Configurable(name = "content side")
    public IO internalContentIO = IO.BOTH;

    @Override
    protected void process() {
        out = in;
        if (in != null) {
            var copied = false;
            var io = contentIO == null ? internalContentIO : contentIO;
            if (contentModifier != null && !contentModifier.isIdentity() && io != IO.NONE) {
                out = in.copy(contentModifier, false, io);
                copied = true;
            }
            if (durationModifier != null && !durationModifier.isIdentity()) {
                if (copied) {
                    out.duration = durationModifier.apply(out.duration).intValue();
                } else {
                    out = in.copy(durationModifier, true, IO.NONE);
                }
            }
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("contentIO")) {
                if (!port.getEdges().isEmpty()) return;
            }
        }
        super.buildConfigurator(father);
    }

}
