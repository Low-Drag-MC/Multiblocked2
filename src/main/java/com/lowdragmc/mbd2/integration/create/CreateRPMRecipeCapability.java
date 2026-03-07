//package com.lowdragmc.mbd2.integration.create;
//
//import com.lowdragmc.lowdraglib2.gui.editor.configurator.ConfiguratorGroup;
//import com.lowdragmc.lowdraglib2.gui.editor.configurator.NumberConfigurator;
//import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
//import com.lowdragmc.lowdraglib2.gui.widget.ImageWidget;
//import com.lowdragmc.lowdraglib2.gui.widget.Widget;
//import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
//import com.lowdragmc.lowdraglib2.jei.IngredientIO;
//import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
//import com.lowdragmc.mbd2.api.recipe.content.Content;
//import com.lowdragmc.mbd2.api.recipe.content.SerializerFloat;
//import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
//import com.simibubi.create.AllBlocks;
//import net.minecraft.network.chat.Component;
//
//import java.util.List;
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
//public class CreateRPMRecipeCapability extends RecipeCapability<Float> {
//    public final static CreateRPMRecipeCapability CAP = new CreateRPMRecipeCapability();
//
//    protected CreateRPMRecipeCapability() {
//        super("create_rpm", SerializerFloat.INSTANCE);
//    }
//
//    @Override
//    public Float createDefaultContent() {
//        return 16f;
//    }
//
//    @Override
//    public Widget createPreviewWidget(Float content) {
//        var previewGroup = new WidgetGroup(0, 0, 18, 18);
//        previewGroup.addWidget(new ImageWidget(1, 1, 16, 16, new ItemStackTexture(AllBlocks.SHAFT.asStack())));
//        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue(content.longValue()));
//        return previewGroup;
//    }
//
//    @Override
//    public Widget createXEITemplate() {
//        return new CreateRPMWidget().setRpm(0);
//    }
//
//    @Override
//    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
//        if (widget instanceof CreateRPMWidget createRPMWidget) {
//            createRPMWidget.setRpm(of(content.content));
//        }
//    }
//
//    @Override
//    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Float> supplier, Consumer<Float> onUpdate) {
//        var configurator = new NumberConfigurator("recipe.capability.create_rpm.rpm", supplier::get,
//                number -> onUpdate.accept(number.floatValue()), 1, true).setRange(0, Float.MAX_VALUE);
//        configurator.setTips("config.kinetic_machine.torque.tooltip.1");
//        father.addConfigurators(configurator);
//    }
//
//    @Override
//    public Component getLeftErrorInfo(List<Float> left) {
//        return Component.literal(left.stream().mapToDouble(Float::doubleValue).sum() + " rpm");
//    }
//}
