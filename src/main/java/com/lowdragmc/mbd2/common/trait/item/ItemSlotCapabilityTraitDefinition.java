package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
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
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@Getter @Setter
@LDLRegister(name = "item_slot", registry = "mbd2:trait_definition_type", priority = -100)
public class ItemSlotCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IItemHandler, @Nullable Direction> implements IUIProviderTrait, ICapabilityProviderTrait {
    @Configurable(name = "config.definition.trait.item_slot.slot_size", tips = "config.definition.trait.item_slot.slot_size.tooltip")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int slotSize = 1;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.item_slot.allow_same_items", tips = "config.definition.trait.item_slot.allow_same_items.tooltip")
    private boolean allowSameItems = true;
    @Configurable(name = "config.definition.trait.item_slot.slot_limit", tips = "config.definition.trait.item_slot.slot_limit.tooltip")
    @ConfigNumber(range = {1, 64})
    private int slotLimit = 64;
    @Configurable(name = "config.definition.trait.item_slot.filter", subConfigurable = true, tips = "config.definition.trait.item_slot.filter.tooltip")
    private final ItemFilterSettings itemFilterSettings = new ItemFilterSettings();
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.item_slot.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();
    @Configurable(name = "config.definition.trait.auto_world_io.input", subConfigurable = true, tips = "config.definition.trait.auto_world_io.input.tooltip")
    private final AutoWorldIO autoWorldInput = new AutoWorldIO();
    @Configurable(name = "config.definition.trait.auto_world_io.output", subConfigurable = true, tips = "config.definition.trait.auto_world_io.output.tooltip")
    private final AutoWorldIO autoWorldOutput = new AutoWorldIO();
    @Configurable(name = "config.definition.trait.item_slot.fancy_renderer", subConfigurable = true, tips = "config.definition.trait.item_slot.fancy_renderer.tooltip")
    private final ItemFancyRendererSettings itemRendererSettings = new ItemFancyRendererSettings(this);

    @Override
    public ItemSlotCapabilityTrait createTrait(MBDMachine machine) {
        return new ItemSlotCapabilityTrait(machine, this);
    }

    @Override
    public BlockCapability<IItemHandler, @Nullable Direction> getCapability() {
        return Capabilities.ItemHandler.BLOCK;
    }

    @Override
    protected @Nullable IItemHandler getCapContent(MBDMachine machine, @Nullable Direction context) {
        return new ItemHandlerList(machine.getAdditionalTraits().stream().filter(trait -> trait instanceof ItemSlotCapabilityTrait)
                .map(ItemSlotCapabilityTrait.class::cast)
                .map(trait -> trait.getCapContent(trait.getCapabilityIO(context)))
                .toArray(IItemHandler[]::new));
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.CHEST);
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var prefix = uiId();
        for (var i = 0; i < this.slotSize; i++) {
            var slot = new ItemSlot();
            slot.setId(prefix + "_" + i);
            container.addChild(slot);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof ItemSlotCapabilityTrait itemSlotTrait) {
            var prefix = uiId();
            var guiIO = getGuiIO();
            ui.selectRegex("^%s_[0-9]+$".formatted(prefix), ItemSlot.class).forEach(itemSlot -> {
                var idStr = itemSlot.getId();
                var lastUnderscore = idStr.lastIndexOf('_');
                if (lastUnderscore < 0) return;
                int index;
                try {
                    index = Integer.parseInt(idStr.substring(lastUnderscore + 1));
                } catch (NumberFormatException e) {
                    return;
                }
                if (index >= 0 && index < itemSlotTrait.storage.getSlots()) {
                    itemSlot.bind(itemSlotTrait.storage, index);
                    if (itemSlot.getSlot() instanceof ItemHandlerSlot handlerSlot) {
                        if (!guiIO.support(IO.IN)) {
                            handlerSlot.setCanPlace(stack -> false);
                        }
                        if (!guiIO.support(IO.OUT)) {
                            handlerSlot.setCanTake(player -> false);
                        }
                    }
                }
            });
        }
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return itemRendererSettings.getFancyRenderer(machine);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInEditor(MultiBufferSource bufferSource, float partialTicks) {
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines());
        var poseStack = new PoseStack();
        if (autoWorldOutput.isEnable()) {
            var aabb = autoWorldOutput.getRange();
            var color = 0xffee6500;
            RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                    (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ,
                    (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ,
                    ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
        }
        if (autoWorldInput.isEnable()) {
            var aabb = autoWorldInput.getRange();
            var color = 0xff11aaee;
            RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                    (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ,
                    (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ,
                    ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
        }
    }
}
