package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.client.renderer.custom.CustomRenderer;
import com.lowdragmc.mbd2.client.renderer.custom.CustomRendererRegistry;
import com.lowdragmc.mbd2.client.renderer.custom.MachineRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;

/**
 * Delegates block entity rendering to a {@link CustomRenderer} registered under {@link #rendererName},
 * usually from a KubeJS client script.
 * <p>
 * The lookup happens per frame instead of being resolved once, so editing the script and reloading client
 * resources takes effect without touching the machine definition.
 */
@LDLRegisterClient(name = "custom_script", registry = "ldlib2:renderer", modID = "kubejs")
@OnlyIn(Dist.CLIENT)
public class CustomScriptRenderer implements IRenderer {
    /**
     * Not {@link Configurable} because the configurator below offers the registered names instead of a free
     * form resource location, but it still has to round trip through {@link IRenderer#copy()} and the machine file.
     */
    @Getter
    @Persisted
    private ResourceLocation rendererName = MBD2.id("missing");

    /**
     * Free form parameters handed to the renderer, so that a single script can back several machines.
     */
    @Getter
    @Configurable(name = "config.renderer.custom_script.data", tips = "config.renderer.custom_script.data.tooltip")
    private CompoundTag data = new CompoundTag();

    public void setRendererName(ResourceLocation rendererName) {
        this.rendererName = rendererName == null ? MBD2.id("missing") : rendererName;
    }

    public void setData(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    @Override
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        var renderer = CustomRendererRegistry.get(rendererName);
        if (renderer == null) return;
        // hand the script a stack of its own, seeded with the current transform. An unbalanced pushPose in a
        // script would otherwise trip vanilla's "Pose stack not empty" check and take the whole game down.
        var scriptStack = new PoseStack();
        scriptStack.last().pose().set(stack.last().pose());
        scriptStack.last().normal().set(stack.last().normal());
        try {
            renderer.render(new MachineRenderContext(blockEntity, data, partialTicks, scriptStack, buffer,
                    combinedLight, combinedOverlay));
        } catch (Throwable throwable) {
            CustomRendererRegistry.markBroken(rendererName, throwable);
        }
    }

    @Override
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        var renderer = CustomRendererRegistry.get(rendererName);
        return renderer != null && Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, renderer.getViewDistance());
    }

    @Override
    public int getViewDistance() {
        var renderer = CustomRendererRegistry.get(rendererName);
        return renderer == null ? IRenderer.super.getViewDistance() : renderer.getViewDistance();
    }

    @Override
    public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
        var renderer = CustomRendererRegistry.get(rendererName);
        return renderer != null && renderer.shouldRenderOffScreen();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        var renderer = CustomRendererRegistry.get(rendererName);
        return renderer == null
                ? IRenderer.super.getRenderBoundingBox(blockEntity)
                : renderer.getRenderBoundingBox(blockEntity, data);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IRenderer.super.buildConfigurator(father);
        var candidates = new ArrayList<>(CustomRendererRegistry.getNames());
        // keep the configured name selectable even when its script is not loaded, otherwise opening the editor
        // would silently rewrite the machine to whatever happens to be first in the list.
        if (!candidates.contains(rendererName)) {
            candidates.add(0, rendererName);
        }
        father.addConfiguratorAt(new SelectorConfigurator<>("config.renderer.custom_script.renderer_name",
                this::getRendererName, this::setRendererName, rendererName, true, candidates, ResourceLocation::toString)
                .setTips("config.renderer.custom_script.renderer_name.tooltip"), 1);
    }
}
