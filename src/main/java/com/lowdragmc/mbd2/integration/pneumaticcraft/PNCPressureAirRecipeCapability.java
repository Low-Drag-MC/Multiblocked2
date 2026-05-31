package com.lowdragmc.mbd2.integration.pneumaticcraft;

import com.lowdragmc.lowdraglib2.configurator.ui.BooleanConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import me.desht.pneumaticcraft.common.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PNCPressureAirRecipeCapability extends RecipeCapability<PressureAir> {
    @LDLRegister(name = "pneumatic_pressure_air", registry = "mbd2:recipe_capability", modID = "pneumaticcraft")
    public static final PNCPressureAirRecipeCapability CAP = new PNCPressureAirRecipeCapability();

    protected PNCPressureAirRecipeCapability() {
        super("pneumatic_pressure_air", PressureAir.SerializerPressureAir.INSTANCE);
    }

    @Override
    public PressureAir createDefaultContent() {
        return new PressureAir(false, 100);
    }

    @Override
    public UIElement createPreview(Supplier<PressureAir> content) {
        return new UIElement()
                .style(style -> style.background(new ItemStackTexture(new ItemStack(ModItems.PRESSURE_GAUGE.get()))))
                .layout(layout -> layout.width(18).height(18))
                .addChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() ->
                                Component.literal(String.valueOf((long) of(content.get()).value()))))
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
        progress.getLayout().height(14);
        progress.barContainer.getLayout().paddingAll(0);
        progress.barContainer.getStyle().background(MCSprites.RECT_1);
        progress.bar.getStyle().background(MBDSprites.PRESSURE_AIR_BAR);
        progress.setProgress(1f);
        progress.label.setText("0 pressure");
        return progress;
    }

    @Override
    public void bindXEIWidget(UIElement widget, Content content, IO io) {
        if (widget instanceof ProgressBar progressBar) {
            var pressureAir = of(content.content);
            var unit = LocalizationUtils.format(pressureAir.isAir() ?
                    "recipe.capability.pneumatic_pressure_air.type.air" :
                    "recipe.capability.pneumatic_pressure_air.type.pressure");
            progressBar.label.setText(pressureAir.value() + " " + unit + (content.perTick ? "/t" : ""));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<PressureAir> supplier, Consumer<PressureAir> onUpdate) {
        father.addConfigurators(
                new BooleanConfigurator("recipe.capability.pneumatic_pressure_air.is_air",
                        () -> supplier.get().isAir(),
                        isAir -> onUpdate.accept(new PressureAir(isAir, supplier.get().value())),
                        false, true).setTips("recipe.capability.pneumatic_pressure_air.is_air.tooltip"),
                new NumberConfigurator("recipe.capability.pneumatic_pressure_air.value",
                        () -> supplier.get().value(),
                        number -> onUpdate.accept(new PressureAir(supplier.get().isAir(), number.floatValue())),
                        1, true).setRange(1, Float.MAX_VALUE)
        );
    }

    @Override
    public Component getLeftErrorInfo(List<PressureAir> left) {
        float airValue = 0f;
        float pressureValue = 0f;
        for (PressureAir pressureAir : left) {
            if (pressureAir.isAir()) {
                airValue += pressureAir.value();
            } else {
                pressureValue += pressureAir.value();
            }
        }
        return Component.literal("[")
                .append(Component.translatable("recipe.capability.pneumatic_pressure_air.type.air"))
                .append(Component.literal(": " + airValue + "], ["))
                .append(Component.translatable("recipe.capability.pneumatic_pressure_air.type.pressure"))
                .append(Component.literal(": " + pressureValue + "]"));
    }
}
