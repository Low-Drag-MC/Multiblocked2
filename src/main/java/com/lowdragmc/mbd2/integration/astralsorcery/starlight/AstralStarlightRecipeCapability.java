package com.lowdragmc.mbd2.integration.astralsorcery.starlight;

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
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Starlight delivered by lenses, optionally of one constellation. Input only: only focus crystals feed Astral
 * Sorcery's network, so a recipe outputting starlight never finds a handler.
 */
public class AstralStarlightRecipeCapability extends RecipeCapability<StarlightAmount> {
    @LDLRegister(name = "astral_starlight", registry = "mbd2:recipe_capability", modID = AstralSorceryIntegration.MOD_ID)
    public static final AstralStarlightRecipeCapability CAP = new AstralStarlightRecipeCapability();

    protected AstralStarlightRecipeCapability() {
        super("astral_starlight", SerializerStarlightAmount.INSTANCE);
    }

    @Override
    public StarlightAmount createDefaultContent() {
        return StarlightAmount.any(SerializerStarlightAmount.DEFAULT_AMOUNT);
    }

    @Override
    public UIElement createPreview(Supplier<StarlightAmount> content) {
        return new UIElement()
                .style(style -> style.background(MBDSprites.ASTRAL_STARLIGHT))
                .layout(layout -> layout.width(18).height(18))
                .addChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() ->
                                Component.literal(format(of(content.get()).amount()))))
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
                .style(style -> style.background(MBDSprites.ASTRAL_STARLIGHT));
        var label = new Label();
        label.setText("0 starlight").addClass("starlight-label");
        label.textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL));
        label.setOverflowVisible(false);
        label.layout(layout -> layout.flex(1).height(10));
        ui.addChildren(icon, label);
        return ui;
    }

    /** The widget sits next to the starlight icon, so it names only the constellation; the tooltip has the full text. */
    @Override
    public void bindXEIWidget(UIElement ui, Content content, IO io) {
        var amount = of(content.content);
        ui.style(style -> style.tooltips(describe(amount, io, content.perTick)));
        ui.select(".starlight-label", Label.class).forEach(label -> label.setText(describe(amount, io, content.perTick, true)));
    }

    private static Component describe(StarlightAmount content, IO io, boolean perTick) {
        return describe(content, io, perTick, false);
    }

    private static Component describe(StarlightAmount content, IO io, boolean perTick, boolean compact) {
        var amount = (io == IO.IN ? "-" : io == IO.OUT ? "+" : "") + format(content.amount()) + (perTick ? "/t" : "");
        if (content.constellation().isEmpty()) {
            return Component.translatable("recipe.capability.astral_starlight.amount", amount);
        }
        var id = content.constellation().get();
        var constellation = AstralSorceryIntegration.constellation(id);
        var name = AstralSorceryIntegration.constellationName(id);
        var text = compact
                ? Component.literal(amount + " ").append(name)
                : Component.translatable("recipe.capability.astral_starlight.amount_of", amount, name);
        return constellation == null ? text : text.withColor(AstralSorceryIntegration.constellationColor(constellation));
    }

    /** Whole amounts without a fraction, the rest to two places. */
    public static String format(float amount) {
        return amount == Math.rint(amount) ? String.valueOf((long) amount) : String.format(Locale.ROOT, "%.2f", amount);
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<StarlightAmount> supplier, Consumer<StarlightAmount> onUpdate) {
        father.addConfigurators(
                AstralSorceryIntegration.idSearch("recipe.capability.astral_starlight.constellation",
                        () -> supplier.get().constellation().orElse(AstralSorceryIntegration.ANY),
                        id -> onUpdate.accept(new StarlightAmount(AstralSorceryIntegration.ANY.equals(id)
                                ? Optional.empty() : Optional.of(id), supplier.get().amount())),
                        AstralSorceryIntegration.ANY,
                        AstralSorceryIntegration::constellationIdsOrAny,
                        AstralSorceryIntegration::constellationOrAnyName),
                new NumberConfigurator("recipe.capability.astral_starlight.amount_name",
                        () -> supplier.get().amount(),
                        number -> onUpdate.accept(supplier.get().withAmount(Math.max(0.01f, number.floatValue()))),
                        SerializerStarlightAmount.DEFAULT_AMOUNT, true).setRange(0.01f, Float.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<StarlightAmount> left) {
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
