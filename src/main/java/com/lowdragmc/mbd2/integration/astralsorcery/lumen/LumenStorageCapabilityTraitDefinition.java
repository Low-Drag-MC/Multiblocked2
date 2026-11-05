package com.lowdragmc.mbd2.integration.astralsorcery.lumen;

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
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralTraitUI;
import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LumenStorageCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<ILumenHandler, @Nullable Direction> {
    @LDLRegister(name = "astral_lumen_storage", registry = "mbd2:trait_definition_type", group = "trait",
            priority = -100, modID = AstralSorceryIntegration.MOD_ID)
    public static final SimpleCapabilityTraitDefinition.Type<ILumenHandler, @Nullable Direction, LumenStorageCapabilityTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("astral_lumen_storage", "trait") {
                @Override
                public LumenStorageCapabilityTraitDefinition createDefinition() {
                    return new LumenStorageCapabilityTraitDefinition();
                }

                @Override
                protected BlockCapability<ILumenHandler, @Nullable Direction> getCapability() {
                    return ILumenHandler.BLOCK;
                }

                @Override
                protected ILumenHandler merge(List<ILumenHandler> contents) {
                    return new LumenHandlerList(contents.toArray(ILumenHandler[]::new));
                }
            };

    /** Per lumen type; a lumen array holds 4000. */
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_lumen_storage.capacity",
            tips = "config.definition.trait.astral_lumen_storage.capacity.tooltip")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int capacity = 4000;
    @Getter
    @Configurable(name = "config.definition.trait.astral_lumen_storage.lumen_types",
            tips = {"config.definition.trait.astral_lumen_storage.lumen_types.tooltip.0",
                    "config.definition.trait.astral_lumen_storage.lumen_types.tooltip.1"})
    @ConfigList(configuratorMethod = "configureLumenType", addDefaultMethod = "addDefaultLumenType")
    private final List<ResourceLocation> lumenTypes = new ArrayList<>();
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_lumen_storage.max_receive",
            tips = "config.definition.trait.astral_lumen_storage.max_receive.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int maxReceive = 4000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_lumen_storage.max_extract",
            tips = "config.definition.trait.astral_lumen_storage.max_extract.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int maxExtract = 4000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_lumen_storage.provide_to_network",
            tips = {"config.definition.trait.astral_lumen_storage.provide_to_network.tooltip.0",
                    "config.definition.trait.astral_lumen_storage.provide_to_network.tooltip.1"})
    private boolean provideToNetwork = true;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_lumen_storage.draw_from_network",
            tips = {"config.definition.trait.astral_lumen_storage.draw_from_network.tooltip.0",
                    "config.definition.trait.astral_lumen_storage.draw_from_network.tooltip.1"})
    private boolean drawFromNetwork = false;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_lumen_storage.draw_interval",
            tips = "config.definition.trait.astral_lumen_storage.draw_interval.tooltip")
    @ConfigNumber(range = {1, 1200})
    private int drawInterval = 20;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.astral_lumen_storage.transfer_particles",
            tips = "config.definition.trait.astral_lumen_storage.transfer_particles.tooltip")
    private boolean transferParticles = true;

    @Override
    public LumenStorageCapabilityTrait createTrait(MBDMachine machine) {
        return new LumenStorageCapabilityTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ASTRAL_LUMEN;
    }

    /** The lumen network holds one node per position; two traits would fight over what it advertises. */
    @Override
    public boolean allowMultiple() {
        return false;
    }

    private Configurator configureLumenType(Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
        return AstralSorceryIntegration.idSearch("config.definition.trait.astral_lumen_storage.lumen_type",
                getter, setter, AstralSorceryIntegration.defaultLumenId(),
                AstralSorceryIntegration::lumenIds, AstralSorceryIntegration::lumenName);
    }

    private ResourceLocation addDefaultLumenType() {
        return AstralSorceryIntegration.defaultLumenId();
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
        if (trait instanceof LumenStorageCapabilityTrait lumenTrait) {
            AstralTraitUI.bindBars(ui, uiId(), () -> lumenTrait.getStorage().uiSnapshot(),
                    LumenStorageCapabilityTraitDefinition::bars);
        }
    }

    /** One bar per filtered type, or per stored type without a filter; a type still held after the filter narrowed stays. */
    private static List<AstralTraitUI.Bar> bars(CompoundTag snapshot) {
        var capacity = snapshot.getInt("capacity");
        var amounts = new LinkedHashMap<Lumen, Integer>();
        for (var element : snapshot.getList("filter", Tag.TAG_STRING)) {
            var lumen = AstralSorceryIntegration.lumen(ResourceLocation.tryParse(element.getAsString()));
            if (lumen != null) amounts.put(lumen, 0);
        }
        for (var element : snapshot.getList("contents", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var lumen = AstralSorceryIntegration.lumen(ResourceLocation.tryParse(entry.getString("lumen")));
            if (lumen != null) amounts.put(lumen, entry.getInt("amount"));
        }
        if (amounts.isEmpty()) {
            return List.of(AstralTraitUI.emptyBar(MBDSprites.ASTRAL_LUMEN, "0/" + capacity,
                    Component.translatable("recipe.capability.astral_lumen.empty")));
        }
        var bars = new ArrayList<AstralTraitUI.Bar>();
        amounts.forEach((lumen, amount) -> {
            var color = AstralSorceryIntegration.lumenColor(lumen);
            bars.add(new AstralTraitUI.Bar(AstralSorceryIntegration.lumenId(lumen).toString(), AstralTraitUI.lumenIcon(lumen), color,
                    capacity > 0 ? (float) amount / capacity : 0,
                    Component.literal(amount + "/" + capacity).withColor(color),
                    Component.translatable("recipe.capability.astral_lumen.stored", lumen.getName(), amount, capacity).withColor(color)));
        });
        return bars;
    }
}
