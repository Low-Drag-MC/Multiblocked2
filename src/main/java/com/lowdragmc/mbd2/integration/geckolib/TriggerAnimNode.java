//package com.lowdragmc.mbd2.integration.geckolib;
//
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.Configurable;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.LDLRegister;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.NumberRange;
//import com.lowdragmc.lowdraglib2.gui.editor.configurator.ConfiguratorGroup;
//import com.lowdragmc.lowdraglib2.gui.editor.runtime.ConfiguratorParser;
//import com.lowdragmc.lowdraglib2.gui.graphprocessor.annotation.InputPort;
//import com.lowdragmc.lowdraglib2.gui.graphprocessor.data.NodePort;
//import com.lowdragmc.lowdraglib2.gui.graphprocessor.data.trigger.LinearTriggerNode;
//import com.lowdragmc.mbd2.common.machine.MBDMachine;
//
//import java.lang.reflect.Method;
//import java.util.HashMap;
//
//@LDLRegister(name = "trigger geckolib anima", group = "graph_processor.node.mbd2.machine.geckolib", modID = "geckolib")
//public class TriggerAnimNode extends LinearTriggerNode {
//    @InputPort
//    public MBDMachine machine;
//    @InputPort(name = "animation name")
//    public String animName;
//    @InputPort(name = "animation speed")
//    public Float speed;
//    @Configurable(name = "animation speed")
//    @ConfigNumber(range = {0, Float.MAX_VALUE})
//    public float internalSpeed = 1f;
//
//    @Override
//    protected void process() {
//        if (machine != null && animName != null) {
//            machine.triggerGeckolibAnim(animName, speed == null ? internalSpeed : speed);
//        }
//    }
//
//    @Override
//    public void buildConfigurator(ConfiguratorGroup father) {
//        HashMap<String, Method> setter = new HashMap<>();
//        var clazz = this.getClass();
//        for(NodePort port : this.getInputPorts()) {
//            if (port.fieldName.equals("speed")) {
//                if (port.getEdges().isEmpty()) {
//                    try {
//                        ConfiguratorParser.createFieldConfigurator(clazz.getField("internalSpeed"), father, clazz, setter, this);
//                    } catch (NoSuchFieldException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//        }
//    }
//}
