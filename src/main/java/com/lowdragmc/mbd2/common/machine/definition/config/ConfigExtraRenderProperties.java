package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleObject;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Accessors(fluent = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigExtraRenderProperties extends ToggleObject<List<IRenderer>> {
    protected List<IRenderer> renderers = new ArrayList<>();
    @Getter
    @Setter
    protected boolean enable;

    @Override
    public boolean isEnable() {
        return enable;
    }

    @Override
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        renderers.forEach(r -> r.render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay));
    }

    @Override
    public List<IRenderer> getValue() {
        return renderers;
    }

    @Override
    public void setValue(List<IRenderer> value) {
        renderers = new ArrayList<>(value);
    }
}
