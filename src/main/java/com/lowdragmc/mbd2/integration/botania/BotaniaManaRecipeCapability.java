package com.lowdragmc.mbd2.integration.botania;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
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
import net.minecraft.network.chat.Component;
import vazkii.botania.common.block.BotaniaBlocks;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BotaniaManaRecipeCapability extends RecipeCapability<Integer> {
    @LDLRegister(name = "botania_mana", registry = "mbd2:recipe_capability", modID = "botania")
    public static final BotaniaManaRecipeCapability CAP = new BotaniaManaRecipeCapability();
    public static final SpriteTexture HUD_BACKGROUND = SpriteTexture.of("mbd2:textures/gui/mana_hud.png").setSprite(0, 0, 102, 5);
    public static final SpriteTexture HUD_BAR = SpriteTexture.of("mbd2:textures/gui/mana_hud.png").setSprite(0, 5, 102, 5);

    protected BotaniaManaRecipeCapability() {
        super("botania_mana", SerializerInteger.INSTANCE);
    }

    @Override
    public Integer createDefaultContent() {
        return 512;
    }

    @Override
    public UIElement createPreview(Supplier<Integer> content) {
        return new UIElement()
                .style(style -> style.background(new ItemStackTexture(BotaniaBlocks.manaPool.asItem())))
                .layout(layout -> layout.width(18).height(18))
                .addChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() -> Component.literal(String.valueOf(of(content.get())))))
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
        progress.barContainer.getStyle().background(HUD_BACKGROUND);
        progress.bar.getStyle().background(HUD_BAR.copy().setColor(ColorPattern.LIGHT_BLUE.color));
        progress.setProgress(1f);
        progress.label.setText("0 mana");
        return progress;
    }

    @Override
    public void bindXEIWidget(UIElement element, Content content, IO io) {
        if (element instanceof ProgressBar progressBar) {
            var energy = of(content.content);
            progressBar.label.setText(energy + (content.perTick ? " mana/t" : " mana"));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Integer> supplier, Consumer<Integer> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.botania_mana.mana", supplier::get,
                number -> onUpdate.accept(number.intValue()), 1, true).setRange(1, Integer.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Integer> left) {
        return Component.literal(left.stream().mapToInt(Integer::intValue).sum() + " mana");
    }
}
