package com.lowdragmc.mbd2.integration.astralsorcery.starlight;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralTraitUI;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StarlightReceiverTraitDefinition extends RecipeCapabilityTraitDefinition implements IUIProviderTrait {
    @LDLRegister(name = "astral_starlight_receiver", registry = "mbd2:trait_definition_type", group = "trait",
            modID = AstralSorceryIntegration.MOD_ID)
    public static final TraitDefinitionType<StarlightReceiverTraitDefinition> TYPE =
            new TraitDefinitionType<>("astral_starlight_receiver", "trait") {
                @Override
                public StarlightReceiverTraitDefinition createDefinition() {
                    return new StarlightReceiverTraitDefinition();
                }
            };

    /** Per constellation. */
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_starlight_receiver.capacity",
            tips = "config.definition.trait.astral_starlight_receiver.capacity.tooltip")
    @ConfigNumber(range = {1, Float.MAX_VALUE})
    private float capacity = 1000;
    @Getter
    @Configurable(name = "config.definition.trait.astral_starlight_receiver.constellations",
            tips = "config.definition.trait.astral_starlight_receiver.constellations.tooltip")
    @ConfigList(configuratorMethod = "configureConstellation", addDefaultMethod = "addDefaultConstellation")
    private final List<ResourceLocation> constellations = new ArrayList<>();

    @Override
    public StarlightReceiverTrait createTrait(MBDMachine machine) {
        return new StarlightReceiverTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ASTRAL_STARLIGHT;
    }

    /** The node hands each packet to every receiver trait at its position, so two would each keep all of it. */
    @Override
    public boolean allowMultiple() {
        return false;
    }

    private Configurator configureConstellation(Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
        return AstralSorceryIntegration.idSearch("config.definition.trait.astral_starlight_receiver.constellation",
                getter, setter, AstralSorceryIntegration.defaultConstellationId(),
                AstralSorceryIntegration::constellationIds, AstralSorceryIntegration::constellationName);
    }

    private ResourceLocation addDefaultConstellation() {
        return AstralSorceryIntegration.defaultConstellationId();
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        AstralTraitUI.createBarsTemplate(container, uiId());
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof StarlightReceiverTrait receiver) {
            AstralTraitUI.bindBars(ui, uiId(), () -> receiver.getBuffer().uiSnapshot(),
                    StarlightReceiverTraitDefinition::bars);
        }
    }

    /** As the lumen storage's: one bar per filtered constellation, or per stored one without a filter. */
    private static List<AstralTraitUI.Bar> bars(CompoundTag snapshot) {
        var capacity = snapshot.getFloat("capacity");
        var capacityText = AstralStarlightRecipeCapability.format(capacity);
        var amounts = new LinkedHashMap<ResourceLocation, Float>();
        for (var element : snapshot.getList("filter", Tag.TAG_STRING)) {
            var id = ResourceLocation.tryParse(element.getAsString());
            if (id != null) amounts.put(id, 0f);
        }
        for (var element : snapshot.getList("contents", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var id = ResourceLocation.tryParse(entry.getString("constellation"));
            if (id != null) amounts.put(id, entry.getFloat("amount"));
        }
        if (amounts.isEmpty()) {
            return List.of(AstralTraitUI.emptyBar(MBDSprites.ASTRAL_STARLIGHT, "0/" + capacityText,
                    Component.translatable("recipe.capability.astral_starlight.empty")));
        }
        var bars = new ArrayList<AstralTraitUI.Bar>();
        amounts.forEach((id, amount) -> {
            var constellation = AstralSorceryIntegration.constellation(id);
            var color = constellation == null ? 0xFFFFFFFF : AstralSorceryIntegration.constellationColor(constellation);
            var amountText = AstralStarlightRecipeCapability.format(amount);
            bars.add(new AstralTraitUI.Bar(id.toString(), AstralTraitUI.starIcon(color), color,
                    capacity > 0 ? amount / capacity : 0,
                    Component.literal(amountText + "/" + capacityText).withColor(color),
                    Component.translatable("recipe.capability.astral_starlight.stored",
                            AstralSorceryIntegration.constellationName(id), amountText, capacityText).withColor(color)));
        });
        return bars;
    }
}
