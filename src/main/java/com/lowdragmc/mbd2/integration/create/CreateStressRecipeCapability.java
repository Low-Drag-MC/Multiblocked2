package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerFloat;
import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
import com.simibubi.create.AllBlocks;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CreateStressRecipeCapability extends RecipeCapability<Float> {
    public final static CreateStressRecipeCapability CAP = new CreateStressRecipeCapability();

    protected CreateStressRecipeCapability() {
        super("create_stress", SerializerFloat.INSTANCE);
    }

    @Override
    public Float createDefaultContent() {
        return 512f;
    }

    @Override
    public Widget createPreviewWidget(Float content) {
        var previewGroup = new WidgetGroup(0, 0, 18, 18);
        previewGroup.addWidget(new ImageWidget(1, 1, 16, 16, new ItemStackTexture(AllBlocks.COGWHEEL.asStack())));
        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue(content.longValue()));
        return previewGroup;
    }

    @Override
    public Widget createXEITemplate() {
        return new CreateStressWidget().setStress(0);
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof CreateStressWidget createStressWidget) {
            createStressWidget.setStress(of(content.content));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Float> supplier, Consumer<Float> onUpdate) {
        var configurator = new NumberConfigurator("recipe.capability.create_stress.stress", supplier::get,
                number -> onUpdate.accept(number.floatValue()), 1, true).setRange(0, Float.MAX_VALUE);
        configurator.setTips("config.kinetic_machine.torque.tooltip.1");
        father.addConfigurators(configurator);
    }

    @Override
    public Component getLeftErrorInfo(List<Float> left) {
        return Component.literal(left.stream().mapToDouble(Float::doubleValue).sum() + " stress");
    }
}
