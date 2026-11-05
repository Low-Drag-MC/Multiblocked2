package com.lowdragmc.mbd2.integration.astralsorcery.lumen;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Astral Sorcery lumen: a type and an integer amount, handled by {@link LumenStorageCapabilityTrait}. */
public class AstralLumenRecipeCapability extends RecipeCapability<LumenAmount> {
    @LDLRegister(name = "astral_lumen", registry = "mbd2:recipe_capability", modID = AstralSorceryIntegration.MOD_ID)
    public static final AstralLumenRecipeCapability CAP = new AstralLumenRecipeCapability();

    protected AstralLumenRecipeCapability() {
        super("astral_lumen", SerializerLumenAmount.INSTANCE);
    }

    @Override
    public LumenAmount createDefaultContent() {
        return new LumenAmount(AstralSorceryIntegration.defaultLumenId(), SerializerLumenAmount.DEFAULT_AMOUNT);
    }

    @Override
    public UIElement createPreview(Supplier<LumenAmount> content) {
        return new UIElement()
                .style(style -> style.background(MBDSprites.ASTRAL_LUMEN))
                .layout(layout -> layout.width(18).height(18))
                .addChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() ->
                                Component.literal(String.valueOf(of(content.get()).amount()))))
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
                .style(style -> style.background(MBDSprites.ASTRAL_LUMEN));
        var label = new Label();
        label.setText("0 lumen").addClass("lumen-label");
        label.textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL));
        label.setOverflowVisible(false);
        label.layout(layout -> layout.flex(1).height(10));
        ui.addChildren(icon, label);
        return ui;
    }

    @Override
    public void bindXEIWidget(UIElement ui, Content content, IO io) {
        var amount = of(content.content);
        ui.select(".lumen-label", Label.class).forEach(label -> label.setText(describe(amount, io, content.perTick)));
    }

    private static Component describe(LumenAmount content, IO io, boolean perTick) {
        var amount = (io == IO.IN ? "-" : io == IO.OUT ? "+" : "") + content.amount() + (perTick ? "/t" : "");
        var text = Component.translatable("recipe.capability.astral_lumen.amount", amount,
                AstralSorceryIntegration.lumenName(content.lumen()));
        var lumen = content.resolve();
        return lumen == null ? text : text.withColor(AstralSorceryIntegration.lumenColor(lumen));
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<LumenAmount> supplier, Consumer<LumenAmount> onUpdate) {
        father.addConfigurators(
                AstralSorceryIntegration.idSearch("recipe.capability.astral_lumen.type",
                        () -> supplier.get().lumen(),
                        id -> onUpdate.accept(new LumenAmount(id, supplier.get().amount())),
                        AstralSorceryIntegration.defaultLumenId(),
                        AstralSorceryIntegration::lumenIds,
                        AstralSorceryIntegration::lumenName),
                new NumberConfigurator("recipe.capability.astral_lumen.amount_name",
                        () -> supplier.get().amount(),
                        number -> onUpdate.accept(supplier.get().withAmount(Math.max(1, number.intValue()))),
                        SerializerLumenAmount.DEFAULT_AMOUNT, true).setRange(1, Integer.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<LumenAmount> left) {
        var result = Component.empty();
        for (int i = 0; i < left.size(); i++) {
            result.append(describe(left.get(i), IO.NONE, false));
            if (i < left.size() - 1) {
                result.append(", ");
            }
        }
        return result;
    }
}
