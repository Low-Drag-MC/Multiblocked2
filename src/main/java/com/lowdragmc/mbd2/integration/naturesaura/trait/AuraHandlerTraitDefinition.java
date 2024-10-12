package com.lowdragmc.mbd2.integration.naturesaura.trait;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.TextTextureWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.naturesaura.NaturesAuraRecipeCapability;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

@LDLRegister(name = "aura_handler", group = "trait")
public class AuraHandlerTraitDefinition extends RecipeCapabilityTraitDefinition<Integer> implements IUIProviderTrait {

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.aura_handler.radius", tips = "config.definition.trait.aura_handler.radius.tips")
    @NumberRange(range = {1, 64})
    private int radius = 20;

    @Override
    public ITrait createTrait(MBDMachine machine) {
        return new AuraHandlerTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.PIG_SPAWN_EGG);
    }

    @Override
    public RecipeCapability<Integer> getRecipeCapability() {
        return NaturesAuraRecipeCapability.CAP;
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var text = new TextTextureWidget(0, 0, 100, 10,
                LocalizationUtils.format("recipe.capability.natures_aura.aura", 0))
                .textureStyle(t -> t.setType(TextTexture.TextType.LEFT));
        text.setId(uiPrefixName());
        ui.addWidget(text);
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof AuraHandlerTrait) {
            var prefix = uiPrefixName();
            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), TextTextureWidget.class, text -> {
                text.setText(() -> {
                    var world = trait.getMachine().getLevel();
                    var pos = trait.getMachine().getPos();
                    return Component.translatable("recipe.capability.natures_aura.aura",
                            IAuraChunk.getAuraInArea(world, pos, ((AuraHandlerTrait) trait).getDefinition().radius));
                });
            });
        }
    }
}
