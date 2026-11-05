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
        AstralTraitUI.createTemplate(container, uiId(), MBDSprites.ASTRAL_STARLIGHT, "starlight-label");
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof StarlightReceiverTrait receiver) {
            AstralTraitUI.bindLabel(ui, uiId(), "starlight-label", () -> receiver.getBuffer().uiSnapshot(),
                    StarlightReceiverTraitDefinition::summary, StarlightReceiverTraitDefinition::details);
        }
    }

    private static Component summary(CompoundTag snapshot) {
        var line = Component.empty();
        for (var element : snapshot.getList("contents", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            if (!line.getSiblings().isEmpty()) line.append(" ");
            line.append(colored(entry, Component.literal(AstralStarlightRecipeCapability.format(entry.getFloat("amount")))));
        }
        return line.getSiblings().isEmpty() ? Component.translatable("recipe.capability.astral_starlight.empty") : line;
    }

    private static List<Component> details(CompoundTag snapshot) {
        var capacity = AstralStarlightRecipeCapability.format(snapshot.getFloat("capacity"));
        var lines = new ArrayList<Component>();
        for (var element : snapshot.getList("contents", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var id = ResourceLocation.tryParse(entry.getString("constellation"));
            if (id == null) continue;
            lines.add(colored(entry, Component.translatable("recipe.capability.astral_starlight.stored",
                    AstralSorceryIntegration.constellationName(id),
                    AstralStarlightRecipeCapability.format(entry.getFloat("amount")), capacity)));
        }
        if (lines.isEmpty()) {
            lines.add(Component.translatable("recipe.capability.astral_starlight.empty"));
        }
        return lines;
    }

    private static Component colored(CompoundTag entry, Component text) {
        var constellation = AstralSorceryIntegration.constellation(ResourceLocation.tryParse(entry.getString("constellation")));
        return constellation == null ? text : text.copy().withColor(AstralSorceryIntegration.constellationColor(constellation));
    }
}
