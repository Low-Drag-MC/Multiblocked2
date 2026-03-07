package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
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
    public void createTraitUITemplate(UI ui) {
        // todo ui

//        var row = Math.ceil(Math.sqrt(slotSize));
//        var prefix = uiPrefixName();
//        for (var i = 0; i < this.slotSize; i++) {
//            var slotWidget = new SlotWidget();
//            slotWidget.setSelfPosition(new Position(10 + i % (int) row * 18, 10 + i / (int) row * 18));
//            slotWidget.initTemplate();
//            slotWidget.setId(prefix + "_" + i);
//            ui.addWidget(slotWidget);
//        }
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
//        if (trait instanceof ItemSlotCapabilityTrait itemSlotTrait) {
//            var prefix = uiPrefixName();
//            var guiIO = getGuiIO();
//            var ingredientIO = guiIO == IO.IN ? IngredientIO.INPUT : guiIO == IO.OUT ? IngredientIO.OUTPUT : guiIO == IO.BOTH ? IngredientIO.BOTH : IngredientIO.RENDER_ONLY;
//            WidgetUtils.widgetByIdForEach(group, "^%s_[0-9]+$".formatted(prefix), SlotWidget.class, slotWidget -> {
//                var index = WidgetUtils.widgetIdIndex(slotWidget);
//                if (index >= 0 && index < itemSlotTrait.storage.getSlots()) {
//                    slotWidget.setHandlerSlot(itemSlotTrait.storage, index);
//                    slotWidget.setIngredientIO(ingredientIO);
//                    slotWidget.setCanTakeItems(guiIO.support(IO.OUT));
//                    slotWidget.setCanPutItems(guiIO.support(IO.IN));
//                }
//            });
//        }
    }

//    @Override
//    public IRenderer getBESRenderer(IMachine machine) {
//        return itemRendererSettings.getFancyRenderer(machine);
//    }

//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public void renderAfterWorldInTraitPanel(MachineTraitPanel panel) {
//        super.renderAfterWorldInTraitPanel(panel);
//        if (!autoInput.enable && !autoOutput.enable) return;
//        var poseStack = new PoseStack();
//        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
//
//        RenderSystem.enableBlend();
//        RenderSystem.disableDepthTest();
//        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//
//        poseStack.pushPose();
//        RenderSystem.disableCull();
//        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
//        RenderSystem.lineWidth(5);
//
//        if (autoOutput.enable) {
//            var color = 0xffee6500;
//            RenderBufferUtils.drawCubeFrame(poseStack, buffer,
//                    (float)autoOutput.range.minX, (float)autoOutput.range.minY, (float)autoOutput.range.minZ,
//                    (float)autoOutput.range.maxX, (float)autoOutput.range.maxY, (float)autoOutput.range.maxZ,
//                    ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
//        }
//
//        if (autoInput.enable) {
//            var color = 0xff11aaee;
//            RenderBufferUtils.drawCubeFrame(poseStack, buffer,
//                    (float) autoInput.range.minX, (float) autoInput.range.minY, (float) autoInput.range.minZ,
//                    (float) autoInput.range.maxX, (float) autoInput.range.maxY, (float) autoInput.range.maxZ,
//                    ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
//        }
//        BufferUploader.drawWithShader(buffer.buildOrThrow());
//
//        poseStack.popPose();
//        RenderSystem.enableDepthTest();
//        RenderSystem.enableCull();
//    }

//    private Configurator buildItemSlotConfigurator(Supplier<ItemSlotConfig> getter, Consumer<ItemSlotConfig> setter) {
//        var configurator = new ConfiguratorGroup().hideTitle();
//        configurator.setCollapse(false);
//        getter.get().buildConfigurator(configurator);
//        return configurator;
//    }
//
//    private ItemSlotConfig addDefaultItemSlot() {
//        return new ItemSlotConfig(this);
//    }
//
//    private IntTag itemSlotSerialize(List<ItemSlotConfig> configs) {
//        return IntTag.valueOf(configs.size());
//    }
//
//    private List<ItemSlotConfig> itemSlotDeserialize(IntTag tag) {
//        var configs = new ArrayList<ItemSlotConfig>();
//        for (int i = 0; i < tag.getAsInt(); i++) {
//            configs.add(addDefaultItemSlot());
//        }
//        return configs;
//    }

}
