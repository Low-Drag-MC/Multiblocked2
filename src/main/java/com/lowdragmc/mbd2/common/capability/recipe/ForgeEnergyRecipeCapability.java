package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerInteger;
import com.lowdragmc.mbd2.utils.EnergyFormattingUtil;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ForgeEnergyRecipeCapability extends RecipeCapability<Integer> {
    @LDLRegister(name = "forge_energy", registry = "mbd2:recipe_capability")
    public final static ForgeEnergyRecipeCapability CAP = new ForgeEnergyRecipeCapability();

    public final static SpriteTexture ENERGY_ICON = SpriteTexture.of("mbd2:textures/gui/forge_energy.png");
    public final static SpriteTexture ENERGY_BG = SpriteTexture.of("mbd2:textures/gui/energy_bar_background.png").setBorder(1).setWrapMode(SpriteTexture.WrapMode.REPEAT);
    public final static SpriteTexture ENERGY_BAR = SpriteTexture.of("mbd2:textures/gui/energy_bar_base.png").setBorder(1).setWrapMode(SpriteTexture.WrapMode.REPEAT);

    protected ForgeEnergyRecipeCapability() {
        super("forge_energy", SerializerInteger.INSTANCE);
    }


    @Override
    public Integer createDefaultContent() {
        return 512;
    }

    @Override
    public UIElement createPreview(Supplier<Integer> content) {
        return new UIElement()
                .style(style -> style.background(ENERGY_ICON))
                .layout(layout -> layout.width(18).height(18))
                .addChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() ->
                                Component.literal(EnergyFormattingUtil.formatCompact(of(content.get()))))
                        )
                        .textStyle(textStyle -> textStyle
                                .textAlignVertical(Vertical.BOTTOM)
                                .textAlignHorizontal(Horizontal.RIGHT)
                                .fontSize(4.5f)
                        )
                        .layout(layout -> layout.widthPercent(100).heightPercent(100))
                );
    }

    @Override
    public UIElement createXEITemplate() {
        var progress = new ProgressBar();
        progress.getLayout().width(100).height(14);
        progress.barContainer.getLayout().paddingAll(0);
        progress.barContainer.getStyle().background(ENERGY_BAR);
        progress.bar.getStyle().background(ENERGY_BG);
        progress.setProgress(1f);
        progress.label.setText("0 FE");
        return progress;
    }

    @Override
    public void bindXEIWidget(UIElement element, Content content, IO ingredientIO) {
        if (element instanceof ProgressBar progressBar) {
            var energy = EnergyFormattingUtil.formatExtended(of(content.content));
            progressBar.label.setText(energy);
            if (content.perTick) {
                progressBar.label.setText(energy + "FE/t");
            } else {
                progressBar.label.setText(energy + "FE");
            }
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Integer> supplier, Consumer<Integer> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.forge_energy.energy", supplier::get,
                number -> onUpdate.accept(number.intValue()), 1, true).setRange(1, Integer.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Integer> left) {
        return Component.literal(left.stream().mapToInt(Integer::intValue).sum() + " fe");
    }
}
