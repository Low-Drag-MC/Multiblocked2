package com.lowdragmc.mbd2.common.gui;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.UIResourceTexture;

import java.lang.reflect.Modifier;

public final class MBDSprites {
    public final static SpriteTexture ICON = SpriteTexture.of("mbd2:textures/icon.png");
    public final static SpriteTexture ENERGY_ICON = SpriteTexture.of("mbd2:textures/gui/forge_energy.png");
    public final static SpriteTexture ENERGY_BG = SpriteTexture.of("mbd2:textures/gui/energy_bar_background.png")
            .setBorder(1).setWrapMode(SpriteTexture.WrapMode.REPEAT);
    public final static SpriteTexture ENERGY_BAR = SpriteTexture.of("mbd2:textures/gui/energy_bar_base.png")
            .setBorder(1).setWrapMode(SpriteTexture.WrapMode.REPEAT);

    public final static SpriteTexture FLUID_SLOT_OVERLAY = SpriteTexture.of("mbd2:textures/gui/fluid_tank_overlay.png");

    public final static SpriteTexture ARROW_BG = SpriteTexture.of("mbd2:textures/gui/arrow_bar.png")
            .setSprite(0, 0, 20, 20);
    public final static SpriteTexture ARROW_BAR = SpriteTexture.of("mbd2:textures/gui/arrow_bar.png")
            .setSprite(0, 20, 20, 20);

    public final static SpriteTexture FUEL_BG = SpriteTexture.of("mbd2:textures/gui/fuel_bar.png")
            .setSprite(0, 0, 18, 18);
    public final static SpriteTexture FUEL_BAR = SpriteTexture.of("mbd2:textures/gui/fuel_bar.png")
            .setSprite(0, 18, 18, 18);

    public final static SpriteTexture PNC_HEAT_BG = SpriteTexture.of("mbd2:textures/gui/pnc_heat_bg.png");
    public final static SpriteTexture PNC_HEAT_BAR = SpriteTexture.of("mbd2:textures/gui/pnc_heat_bar.png");
    public final static SpriteTexture MEK_HEAT_BAR = SpriteTexture.of("mbd2:textures/gui/mek_heat_bar.png");
    public final static SpriteTexture PRESSURE_AIR_BAR = SpriteTexture.of("mbd2:textures/gui/pressure_air_bar.png").setBorder(1);
    public final static SpriteTexture NATURES_AURA = SpriteTexture.of("naturesaura:textures/item/aura_cache.png");
    public final static SpriteTexture ARS_SOURCE = SpriteTexture.of("ars_nouveau:textures/item/source_gem.png");
    public final static SpriteTexture ARS_SOURCE_BAR = SpriteTexture.of("mbd2:textures/gui/ars_source_bar.png")
            .setBorder(1).setWrapMode(SpriteTexture.WrapMode.REPEAT);


    public static void init(ResourceInstance<IGuiTexture> instance) {
        var provider = new BuiltinResourceProvider<>("ui-mbd", instance);
        for (var field : MBDSprites.class.getDeclaredFields()) {
            if (IGuiTexture.class.isAssignableFrom(field.getType())
                    && Modifier.isStatic(field.getModifiers()) ) {
                try {
                    var texture = (IGuiTexture) field.get(null);
                    provider.addResource(field.getName(), texture);
                    var res = new BuiltinPath("ui-mbd:" + field.getName());
                    texture = new UIResourceTexture(res);
                    field.set(null, texture);
                } catch (Exception ignored) {}
            }
        }
        instance.addBuiltinProvider(provider);
    }
}
