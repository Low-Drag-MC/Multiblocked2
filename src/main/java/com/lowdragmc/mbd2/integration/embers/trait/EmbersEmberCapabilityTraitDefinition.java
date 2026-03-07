//package com.lowdragmc.mbd2.integration.embers.trait;
//
//import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.Configurable;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.LDLRegister;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.NumberRange;
//import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ProgressTexture;
//import com.lowdragmc.lowdraglib2.gui.widget.ProgressWidget;
//import com.lowdragmc.lowdraglib2.gui.widget.TextTextureWidget;
//import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
//import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
//import com.lowdragmc.mbd2.api.machine.IMachine;
//import com.lowdragmc.mbd2.common.machine.MBDMachine;
//import com.lowdragmc.mbd2.common.trait.ITrait;
//import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
//import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
//import com.lowdragmc.mbd2.integration.embers.EmbersEmberRecipeCapability;
//import com.lowdragmc.mbd2.utils.WidgetUtils;
//import com.rekindled.embers.RegistryManager;
//import lombok.Getter;
//import lombok.Setter;
//import net.minecraft.network.chat.Component;
//
//@LDLRegister(name = "embers_ember_capability", group = "trait", modID = "embers")
//public class EmbersEmberCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition {
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.embers_ember_capability.capacity")
//    @ConfigNumber(range = {1, Double.MAX_VALUE})
//    private double capacity = 5000;
//    @Getter
//    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.embers_ember_capability.auto_io.tooltip")
//    private final ToggleAutoIO autoIO = new ToggleAutoIO();
//    @Configurable(name = "config.definition.trait.embers_ember_capability.fancy_renderer", subConfigurable = true,
//            tips = "config.definition.trait.embers_ember_capability.fancy_renderer.tooltip")
//    private final EmbersEmberFancyRendererSettings fancyRendererSettings = new EmbersEmberFancyRendererSettings(this);
//
//    @Override
//    public EmbersEmberCapabilityTrait createTrait(MBDMachine machine) {
//        return new EmbersEmberCapabilityTrait(machine, this);
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return new ItemStackTexture(RegistryManager.EMBER_CRYSTAL.get());
//    }
//
//
//    @Override
//    public boolean allowMultiple() {
//        return false;
//    }
//
//    @Override
//    public IRenderer getBESRenderer(IMachine machine) {
//        return fancyRendererSettings.getFancyRenderer(machine);
//    }
//
//    @Override
//    public void createTraitUITemplate(WidgetGroup ui) {
//        var prefix = uiPrefixName();
//        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 10, new ProgressTexture(
//                IGuiTexture.EMPTY, EmbersEmberRecipeCapability.HUD_BAR
//        ));
//        energyBar.setBackground(EmbersEmberRecipeCapability.HUD_BACKGROUND);
//        energyBar.setId(prefix);
//        var energyBarText = new TextTextureWidget(5, 0, 90, 10)
//                .setText("0/0 ember")
//                .textureStyle(textTexture -> textTexture.setDropShadow(true));
//        energyBarText.setId(prefix + "_text");
//        ui.addWidget(energyBar);
//        ui.addWidget(energyBarText);
//    }
//
//    @Override
//    public void initTraitUI(ITrait trait, WidgetGroup group) {
//        if (trait instanceof EmbersEmberCapabilityTrait emberTrait) {
//            var prefix = uiPrefixName();
//            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
//                energyBar.setProgressSupplier(() -> emberTrait.storage.getEmber() / emberTrait.storage.getEmberCapacity());
//                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
//                        "config.definition.trait.embers_ember_capability.ui_container_hover",
//                        Math.round(emberTrait.storage.getEmberCapacity() * value), emberTrait.storage.getEmberCapacity()));
//            });
//            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
//                energyBarText.setText(() -> Component.literal(Math.round(emberTrait.storage.getEmber()) + "/" + emberTrait.storage.getEmberCapacity() + " ember"));
//            });
//        }
//    }
//}
