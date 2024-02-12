package com.lowdragmc.mbd2.common.gui.editor.texture;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class IRendererSlotTexture implements IGuiTexture {

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(GuiGraphics guiGraphics, int i, int i1, float v, float v1, int i2, int i3) {

    }
}
