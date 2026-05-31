package com.lowdragmc.mbd2.client;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.mbd2.MBD2;
import com.simibubi.create.Create;

import java.lang.reflect.Modifier;

public class MBDRenderers {
    public static IRenderer MACHINE = new IModelRenderer(MBD2.id("block/machine"));
    public static IRenderer MACHINE_UNFORMED = new IModelRenderer(MBD2.id("block/machine_unformed"));
    public static IRenderer MACHINE_FORMED = new IModelRenderer(MBD2.id("block/machine_formed"));
    public static IRenderer MACHINE_WAITING = new IModelRenderer(MBD2.id("block/machine_waiting"));
    public static IRenderer MACHINE_WORKING = new IModelRenderer(MBD2.id("block/machine_working"));
    public static IRenderer GEAR_BOX = new IModelRenderer(MBD2.id("block/gearbox"));
    public static IRenderer PEDESTAL = new IModelRenderer(MBD2.id("block/pedestal"));

    public static void init(ResourceInstance<IRenderer> instance) {
        var provider = new BuiltinResourceProvider<>("mbd", instance);
        for (var field : MBDRenderers.class.getDeclaredFields()) {
            if (IRenderer.class.isAssignableFrom(field.getType())
                    && Modifier.isStatic(field.getModifiers()) ) {
                try {
                    var renderer = (IRenderer) field.get(null);
                    provider.addResource(field.getName(), renderer);
                    var res = new BuiltinPath("mbd:" + field.getName());
                    renderer = new UIResourceRenderer(res);
                    field.set(null, renderer);
                } catch (Exception ignored) {}
            }
        }
        if (MBD2.isCreateLoaded()) {
            provider.addResource("SHAFT", new IModelRenderer(Create.asResource("block/shaft")));
            provider.addResource("COG_WHEEL", new IModelRenderer(Create.asResource("block/cogwheel")));
            provider.addResource("COG_WHEEL_SHAFTLESS", new IModelRenderer(Create.asResource("block/cogwheel_shaftless")));
            provider.addResource("LARGE_COG_WHEEL", new IModelRenderer(Create.asResource("block/large_cogwheel")));
            provider.addResource("LARGE_COG_WHEEL_SHAFTLESS", new IModelRenderer(Create.asResource("block/large_cogwheel_shaftless")));
        }
        instance.addBuiltinProvider(provider);
    }
}
