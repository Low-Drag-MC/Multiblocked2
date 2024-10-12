package com.lowdragmc.mbd2.integration.naturesaura;

import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerInteger;
import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
import de.ellpeck.naturesaura.blocks.ModBlocks;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NaturesAuraRecipeCapability extends RecipeCapability<Integer> {
    public final static NaturesAuraRecipeCapability CAP = new NaturesAuraRecipeCapability();
    protected NaturesAuraRecipeCapability() {
        super("natures_aura", SerializerInteger.INSTANCE);
    }

    @Override
    public Integer createDefaultContent() {
        return 512;
    }

    @Override
    public Widget createPreviewWidget(Integer content) {
        var previewGroup = new WidgetGroup(0, 0, 18, 18);
        previewGroup.addWidget(new ImageWidget(1, 1, 16, 16, new ItemStackTexture(ModBlocks.NATURE_ALTAR.asItem())));
        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue(content));
        return previewGroup;
    }

    @Override
    public Widget createXEITemplate() {
        return new TextTextureWidget(0, 0, 100, 10,
                LocalizationUtils.format("recipe.capability.natures_aura.aura", 0))
                .textureStyle(t -> t.setType(TextTexture.TextType.LEFT));
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof TextTextureWidget textTexture) {
            var aura = of(content.content);
            if (content.perTick) {
                textTexture.setText(LocalizationUtils.format("recipe.capability.natures_aura.aura", aura) + "/t");
            } else {
                textTexture.setText(LocalizationUtils.format("recipe.capability.natures_aura.aura", aura));
            }
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Integer> supplier, Consumer<Integer> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.botania_mana.mana", supplier::get,
                number -> onUpdate.accept(number.intValue()), 1, true).setRange(1, Integer.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Integer> left) {
        return Component.literal(left.stream().mapToInt(Integer::intValue).sum() + " aura");
    }
}
