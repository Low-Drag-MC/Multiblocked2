package com.lowdragmc.mbd2.integration.pneumaticcraft;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerDouble;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import me.desht.pneumaticcraft.common.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PNCHeatRecipeCapability extends RecipeCapability<Double> {
    @LDLRegister(name = "pneumatic_heat", registry = "mbd2:recipe_capability", modID = "pneumaticcraft")
    public static final PNCHeatRecipeCapability CAP = new PNCHeatRecipeCapability();

    protected PNCHeatRecipeCapability() {
        super("pneumatic_heat", SerializerDouble.INSTANCE);
    }

    @Override
    public Double createDefaultContent() {
        return 10d;
    }

    @Override
    public UIElement createPreview(Supplier<Double> content) {
        return new UIElement()
                .style(style -> style.background(new ItemStackTexture(new ItemStack(ModItems.HEAT_FRAME.get()))))
                .layout(layout -> layout.width(18).height(18))
                .addChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() ->
                                Component.literal(String.valueOf(of(content.get()).longValue()))))
                        .textStyle(textStyle -> textStyle
                                .textAlignVertical(Vertical.BOTTOM)
                                .textAlignHorizontal(Horizontal.RIGHT)
                                .fontSize(4.5f))
                        .layout(layout -> layout.widthPercent(100).heightPercent(100)));
    }

    @Override
    public XEILayoutType xeiLayoutType() {
        return XEILayoutType.BAR;
    }

    @Override
    public UIElement createXEITemplate() {
        var progress = new ProgressBar();
        progress.getLayout().height(10);
        progress.barContainer.getLayout().paddingAll(0);
        progress.barContainer.getStyle().background(MBDSprites.PNC_HEAT_BG);
        progress.bar.getStyle().background(MBDSprites.PNC_HEAT_BAR);
        progress.setProgress(1f);
        progress.label.setText("0 heat");
        return progress;
    }

    @Override
    public void bindXEIWidget(UIElement widget, Content content, IO io) {
        if (widget instanceof ProgressBar progressBar) {
            var heat = of(content.content).longValue();
            progressBar.label.setText(heat + (content.perTick ? " heat/t" : " heat"));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Double> supplier, Consumer<Double> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.pneumatic_heat.heat", supplier::get,
                number -> onUpdate.accept(number.doubleValue()), 1, true).setRange(1, Double.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Double> left) {
        return Component.literal(left.stream().mapToDouble(Double::doubleValue).sum() + " heat");
    }
}
