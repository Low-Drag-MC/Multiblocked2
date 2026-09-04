package com.lowdragmc.mbd2.integration.arsnouveau;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
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
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Source, the magical resource Ars Nouveau's devices run on.
 *
 * <p>An integer, like energy and aura — Ars Nouveau stores it as a plain {@code int} everywhere
 * ({@code ISourceCap#getSource}), so there is nothing to model beyond an amount.</p>
 *
 * <p>Which machine-side storage a recipe draws this from is the trait's business, not this class's:
 * {@link com.lowdragmc.mbd2.integration.arsnouveau.trait.SourceStorageCapabilityTrait} spends the
 * machine's own buffer, and {@link com.lowdragmc.mbd2.integration.arsnouveau.trait.NearbySourceTrait}
 * reaches into the source jars around it the way an Enchanting Apparatus does. A recipe written
 * against this capability works with either.</p>
 */
public class ArsSourceRecipeCapability extends RecipeCapability<Integer> {
    @LDLRegister(name = "ars_source", registry = "mbd2:recipe_capability", modID = "ars_nouveau")
    public static final ArsSourceRecipeCapability CAP = new ArsSourceRecipeCapability();

    protected ArsSourceRecipeCapability() {
        super("ars_source", SerializerInteger.INSTANCE);
    }

    @Override
    public Integer createDefaultContent() {
        return 100;
    }

    @Override
    public UIElement createPreview(Supplier<Integer> content) {
        return new UIElement()
                .style(style -> style.background(MBDSprites.ARS_SOURCE))
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
        var progress = new ProgressBar();
        progress.getLayout().height(14);
        progress.barContainer.getLayout().paddingAll(0);
        progress.barContainer.getStyle().background(MBDSprites.ENERGY_BG);
        progress.bar.getStyle().background(MBDSprites.ARS_SOURCE_BAR);
        progress.setProgress(1f);
        progress.label.setText("0 source");
        return progress;
    }

    @Override
    public void bindXEIWidget(UIElement element, Content content, IO io) {
        if (element instanceof ProgressBar progressBar) {
            var source = of(content.content);
            progressBar.label.setText(source + (content.perTick ? " source/t" : " source"));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Integer> supplier, Consumer<Integer> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.ars_source.source_name", supplier::get,
                number -> onUpdate.accept(number.intValue()), 1, true).setRange(1, Integer.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Integer> left) {
        return Component.literal(left.stream().mapToInt(Integer::intValue).sum() + " source");
    }
}
