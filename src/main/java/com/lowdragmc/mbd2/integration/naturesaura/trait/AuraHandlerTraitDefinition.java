package com.lowdragmc.mbd2.integration.naturesaura.trait;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.blocks.ModBlocks;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;

public class AuraHandlerTraitDefinition extends RecipeCapabilityTraitDefinition implements IUIProviderTrait {
    @LDLRegister(name = "aura_handler", registry = "mbd2:trait_definition_type", group = "trait", modID = "naturesaura")
    public static final TraitDefinitionType<AuraHandlerTraitDefinition> TYPE =
            new TraitDefinitionType<>("aura_handler", "trait") {
                @Override
                public AuraHandlerTraitDefinition createDefinition() {
                    return new AuraHandlerTraitDefinition();
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.aura_handler.radius", tips = "config.definition.trait.aura_handler.radius.tooltip")
    @ConfigNumber(range = {1, 64})
    private int radius = 20;

    @Override
    public AuraHandlerTrait createTrait(MBDMachine machine) {
        return new AuraHandlerTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(new ItemStack(ModBlocks.NATURE_ALTAR));
    }

    @Override
    public boolean allowMultiple() {
        return false;
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var ui = new UIElement().setId(uiId());
        ui.layout(layout -> layout.gapAll(2).alignItems(AlignItems.CENTER).flexDirection(FlexDirection.ROW));
        var icon = new UIElement()
                .layout(layout -> layout.heightPercent(100).aspectRatio(1))
                .style(style -> style.background(MBDSprites.NATURES_AURA))
                .addClass("aura-icon");
        var label = new Label();
        label.setText("0 aura").addClass("aura-label");
        label.layout(layout -> layout.height(10));
        ui.addChildren(icon, label);
        container.addChild(ui);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof AuraHandlerTrait auraTrait) {
            ui.select("#%s > label".formatted(uiId()), Label.class).forEach(label -> {
                var aura = new AtomicInteger(0);
                var auraSync = DataBindingBuilder.intValS2C(() -> {
                    var level = auraTrait.getMachine().getLevel();
                    var pos = auraTrait.getMachine().getPos();
                    if (level == null) return 0;
                    // the trait's effective radius, not the definition's: an overridden machine draws
                    // from a different area than this would otherwise report
                    return IAuraChunk.getAuraInArea(level, pos, auraTrait.radiusBlocks());
                }).remoteSetter(aura::set).build();
                label.addSyncValue(auraSync.getSyncValue());
                label.bindDataSource(SupplierDataSource.of(() ->
                        Component.translatable("recipe.capability.natures_aura.aura", aura.get())));
            });
        }
    }
}
