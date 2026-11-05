package com.lowdragmc.mbd2.integration.astralsorcery.research;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralTraitUI;
import hellfirepvp.astralsorcery.common.research.ResearchTier;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class AstralResearcherTraitDefinition extends TraitDefinition implements IUIProviderTrait {
    @LDLRegister(name = "astral_researcher", registry = "mbd2:trait_definition_type", group = "trait",
            modID = AstralSorceryIntegration.MOD_ID)
    public static final TraitDefinitionType<AstralResearcherTraitDefinition> TYPE =
            new TraitDefinitionType<>("astral_researcher", "trait") {
                @Override
                public AstralResearcherTraitDefinition createDefinition() {
                    return new AstralResearcherTraitDefinition();
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_researcher.refresh_interval",
            tips = "config.definition.trait.astral_researcher.refresh_interval.tooltip")
    @ConfigNumber(range = {1, 12000})
    private int refreshInterval = 100;

    @Override
    public AstralResearcherTrait createTrait(MBDMachine machine) {
        return new AstralResearcherTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ASTRAL_RESEARCH;
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
        AstralTraitUI.createTemplate(container, uiId(), MBDSprites.ASTRAL_RESEARCH, "researcher-label");
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof AstralResearcherTrait researcher) {
            AstralTraitUI.bindLabel(ui, uiId(), "researcher-label", () -> researcher.getSnapshot().uiSnapshot(),
                    AstralResearcherTraitDefinition::summary, null);
        }
    }

    private static Component summary(CompoundTag snapshot) {
        if (!snapshot.contains("ownerName")) {
            return Component.translatable("config.definition.trait.astral_researcher.unbound");
        }
        Component tier = Component.literal("?");
        if (snapshot.contains("tier")) {
            try {
                tier = ResearchTier.valueOf(snapshot.getString("tier")).getName();
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Component.translatable("config.definition.trait.astral_researcher.bound", snapshot.getString("ownerName"), tier);
    }
}
