//package com.lowdragmc.mbd2.integration.pneumaticcraft;
//
//import com.lowdragmc.lowdraglib2.gui.editor.configurator.ConfiguratorGroup;
//import com.lowdragmc.lowdraglib2.gui.editor.configurator.NumberConfigurator;
//import com.lowdragmc.lowdraglib2.gui.texture.*;
//import com.lowdragmc.lowdraglib2.gui.widget.ImageWidget;
//import com.lowdragmc.lowdraglib2.gui.widget.ProgressWidget;
//import com.lowdragmc.lowdraglib2.gui.widget.Widget;
//import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
//import com.lowdragmc.lowdraglib2.jei.IngredientIO;
//import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
//import com.lowdragmc.mbd2.api.recipe.content.Content;
//import com.lowdragmc.mbd2.api.recipe.content.SerializerDouble;
//import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
//import me.desht.pneumaticcraft.common.core.ModItems;
//import net.minecraft.network.chat.Component;
//
//import java.util.List;
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
//public class PNCHeatRecipeCapability extends RecipeCapability<Double> {
//    public final static PNCHeatRecipeCapability CAP = new PNCHeatRecipeCapability();
//    public static final ResourceTexture HUD_BACKGROUND = new ResourceTexture("mbd2:textures/gui/heat_background.png");
//    public static final ResourceTexture HUD_BAR = new ResourceTexture("mbd2:textures/gui/heat_hud.png");
//    protected PNCHeatRecipeCapability() {
//        super("pneumatic_heat", SerializerDouble.INSTANCE);
//    }
//
//    @Override
//    public Double createDefaultContent() {
//        return 10d;
//    }
//
//    @Override
//    public Widget createPreviewWidget(Double content) {
//        var previewGroup = new WidgetGroup(0, 0, 18, 18);
//        previewGroup.addWidget(new ImageWidget(1, 1, 16, 16,
//                new ItemStackTexture(ModItems.HEAT_FRAME.get())));
//        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue(content.longValue()));
//        return previewGroup;
//    }
//
//    @Override
//    public Widget createXEITemplate() {
//        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 10, new ProgressTexture(
//                IGuiTexture.EMPTY, HUD_BAR.copy()
//        ));
//        energyBar.setBackground(HUD_BACKGROUND);
//        energyBar.setOverlay(new TextTexture("0 heat"));
//        return energyBar;
//    }
//
//    @Override
//    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
//        if (widget instanceof ProgressWidget energyBar) {
//            var energy = of(content.content);
//            if (energyBar.getOverlay() instanceof TextTexture textTexture) {
//                if (content.perTick) {
//                    textTexture.updateText(energy + " heat/t");
//                } else {
//                    textTexture.updateText(energy + " heat");
//                }
//            }
//        }
//    }
//
//    @Override
//    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Double> supplier, Consumer<Double> onUpdate) {
//        father.addConfigurators(new NumberConfigurator("recipe.capability.pneumatic_heat.heat", supplier::get,
//                number -> onUpdate.accept(number.doubleValue()), 1, true).setRange(1, Double.MAX_VALUE));
//    }
//
//    @Override
//    public Component getLeftErrorInfo(List<Double> left) {
//        return Component.literal(left.stream().mapToDouble(Double::doubleValue).sum() + " heat");
//    }
//}
