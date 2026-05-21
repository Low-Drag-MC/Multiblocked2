package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.*;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

@LDLRegister(name = "fluid_tank", registry = "mbd2:trait_definition_type", group = "trait", priority = -100)
public class FluidTankCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IFluidHandler, @Nullable Direction> {

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.tank_size", tips = "config.definition.trait.fluid_tank.tank_size.tooltip")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int tankSize = 1;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.capacity")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int capacity = 1000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.allow_same_fluids", tips = "config.definition.trait.fluid_tank.allow_same_fluids.tooltip")
    private boolean allowSameFluids = true;
    @Getter
    @Configurable(name = "config.definition.trait.fluid_tank.filter", subConfigurable = true, tips = "config.definition.trait.fluid_tank.filter.tooltip")
    private final FluidFilterSettings fluidFilterSettings = new FluidFilterSettings();
    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.fluid_tank.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();
    @Getter
    @Configurable(name = "config.definition.trait.auto_world_io.input", subConfigurable = true, tips = "config.definition.trait.auto_world_io.input.tooltip")
    private final AutoWorldIO autoInput = new AutoWorldIO().setSpeed(1);
    @Getter
    @Configurable(name = "config.definition.trait.auto_world_io.output", subConfigurable = true, tips = "config.definition.trait.auto_world_io.output.tooltip")
    private final AutoWorldIO autoOutput = new AutoWorldIO().setSpeed(1);
    @Configurable(name = "config.definition.trait.fluid_tank.fancy_renderer", subConfigurable = true,
            tips = {"config.definition.trait.fluid_tank.fancy_renderer.tooltip.0", "config.definition.trait.fluid_tank.fancy_renderer.tooltip.1"})
    private final FluidFancyRendererSettings fancyRendererSettings = new FluidFancyRendererSettings(this);

    @Override
    public FluidTankCapabilityTrait createTrait(MBDMachine machine) {
        return new FluidTankCapabilityTrait(machine, this);
    }

    @Override
    protected @Nullable IFluidHandler getCapContent(MBDMachine machine, @Nullable Direction context) {
        return new FluidHandlerList(machine.getAdditionalTraits().stream().filter(trait -> trait instanceof FluidTankCapabilityTrait)
                .map(FluidTankCapabilityTrait.class::cast)
                .map(trait -> trait.getCapContent(trait.getCapabilityIO(context)))
                .toArray(IFluidHandler[]::new));
    }

    @Override
    public BlockCapability<IFluidHandler, @Nullable Direction> getCapability() {
        return Capabilities.FluidHandler.BLOCK;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.WATER_BUCKET);
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var prefix = uiId();
        for (var i = 0; i < this.tankSize; i++) {
            var slot = new FluidSlot();
            slot.getLayout().width(20).height(58);
            slot.getStyle().overlay(SpriteTexture.of("mbd2:textures/gui/fluid_tank_overlay.png"));
            slot.amountLabel.setDisplay(false);
            slot.setId(prefix + "_" + i);
            container.addChild(slot);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof FluidTankCapabilityTrait fluidTankTrait) {
            var prefix = uiId();
            ui.selectRegex("^%s_[0-9]+$".formatted(prefix), FluidSlot.class).forEach(fluidSlot -> {
                var idStr = fluidSlot.getId();
                var lastUnderscore = idStr.lastIndexOf('_');
                if (lastUnderscore < 0) return;
                int index;
                try {
                    index = Integer.parseInt(idStr.substring(lastUnderscore + 1));
                } catch (NumberFormatException e) {
                    return;
                }
                if (index >= 0 && index < fluidTankTrait.storages.length) {
                    fluidSlot.bind(fluidTankTrait.storages[index], 0);
                }
            });
        }
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return fancyRendererSettings.getFancyRenderer(machine);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInEditor(MultiBufferSource bufferSource, float partialTicks) {
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines());
        var poseStack = new PoseStack();
        if (autoOutput.isEnable()) {
            var aabb = autoOutput.getRange();
            var color = 0xffee6500;
            RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                    (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ,
                    (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ,
                    ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
        }
        if (autoInput.isEnable()) {
            var aabb = autoInput.getRange();
            var color = 0xff11aaee;
            RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                    (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ,
                    (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ,
                    ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
        }
    }
}
