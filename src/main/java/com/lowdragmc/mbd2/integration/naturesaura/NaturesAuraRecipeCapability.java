package com.lowdragmc.mbd2.integration.naturesaura;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
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
import com.lowdragmc.mbd2.api.recipe.content.SerializerInteger;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import de.ellpeck.naturesaura.blocks.ModBlocks;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NaturesAuraRecipeCapability extends RecipeCapability<Integer> {
    @LDLRegister(name = "natures_aura", registry = "mbd2:recipe_capability", modID = "naturesaura")
    public static final NaturesAuraRecipeCapability CAP = new NaturesAuraRecipeCapability();

    protected NaturesAuraRecipeCapability() {
        super("natures_aura", SerializerInteger.INSTANCE);
    }

    @Override
    public Integer createDefaultContent() {
        return 512;
    }

    @Override
    public UIElement createPreview(Supplier<Integer> content) {
        return new UIElement()
                .style(style -> style.background(new ItemStackTexture(new ItemStack(ModBlocks.NATURE_ALTAR))))
                .layout(layout -> layout.width(18).height(18))
                .addChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() ->
                                Component.literal(String.valueOf(of(content.get())))))
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
        var ui = new UIElement();
        ui.layout(layout -> layout.height(14).gapAll(2).flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));
        var icon = new UIElement()
                .layout(layout -> layout.heightPercent(100).aspectRatio(1))
                .style(style -> style.background(MBDSprites.NATURES_AURA));
        var label = new Label();
        label.setText("0 aura").addClass("aura-label");
        label.layout(layout -> layout.flex(1).height(10));
        ui.addChildren(icon, label);
        return ui;
    }

    @Override
    public void bindXEIWidget(UIElement ui, Content content, IO io) {
        var aura = of(content.content);
        ui.select(".aura-label", Label.class).forEach(label ->
                label.setText((io == IO.IN ? "-" : "+") + aura + (content.perTick ? " aura/t" : " aura"))
        );
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Integer> supplier, Consumer<Integer> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.natures_aura.aura_name", supplier::get,
                number -> onUpdate.accept(number.intValue()), 1, true).setRange(1, Integer.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Integer> left) {
        return Component.literal(left.stream().mapToInt(Integer::intValue).sum() + " aura");
    }
}
