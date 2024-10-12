package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.runtime.ConfiguratorParser;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;

import java.util.HashMap;

@LDLRegister(name = "recipe modifier", group = "graph_processor.node.mbd2.machine.recipe")
public class RecipeModifierNode extends BaseNode {
    @InputPort
    public Float multiplier;
    @InputPort
    public Float addition;
    @OutputPort
    public ContentModifier modifier;

    @Configurable(name = "multiplier")
    @NumberRange(range = {0, Float.MAX_VALUE})
    public float internalMul = 1;

    @Configurable(name = "addition")
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    public float internalAdd = 0;

    @Override
    protected void process() {
        var mul = multiplier == null ? internalMul : multiplier;
        var add = addition == null ? internalAdd : addition;
        modifier = ContentModifier.of(mul, add);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        var clazz = getClass();
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("multiplier")) {
                if (port.getEdges().isEmpty()) {
                    try {
                        ConfiguratorParser.createFieldConfigurator(clazz.getField("internalMul"), father, clazz, new HashMap<>(), this);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else if (port.fieldName.equals("addition")) {
                if (port.getEdges().isEmpty()) {
                    try {
                        ConfiguratorParser.createFieldConfigurator(clazz.getField("internalAdd"), father, clazz, new HashMap<>(), this);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
