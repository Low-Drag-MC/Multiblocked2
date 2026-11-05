package com.lowdragmc.mbd2.test.tests.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** @see com.lowdragmc.mbd2.test.uitest.JEIRecipeIdScenario */
public class JEIRecipeIdFixtures implements TestFixtureProvider {
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_jei_recipe_ids");
    public static final ResourceLocation PLAIN_ID = MBD2.id("jei_plain_recipe");
    /** Saved as a copy of the plain recipe's file, still carrying its id inside. */
    public static final ResourceLocation COPIED_ID = MBD2.id("jei_copied_recipe");

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        var type = new ItemRecipeType();
        event.register(type);
        type.getBuiltinRecipes().put(PLAIN_ID, type.recipeBuilder(PLAIN_ID)
                .inputItems(Items.IRON_INGOT)
                .outputItems(Items.EMERALD)
                .buildRawRecipe());
        type.getBuiltinRecipes().put(COPIED_ID, type.recipeBuilder(PLAIN_ID)
                .inputItems(Items.GOLD_INGOT)
                .outputItems(Items.DIAMOND)
                .buildRawRecipe());
    }

    /** One item in, one item out, laid out the way the editor's generated recipe UI binds them. */
    private static class ItemRecipeType extends MBDRecipeType {
        ItemRecipeType() {
            super(RECIPE_TYPE_ID);
            var root = new UIElement();
            root.layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(4).paddingAll(4));
            root.addClass("panel_bg");
            for (var io : new IO[]{IO.IN, IO.OUT}) {
                root.addChild(ItemRecipeCapability.CAP.createXEITemplate().setId("@item_" + io.name + "_0"));
            }
            uiTemplate = UITemplate.of(root, StylesheetManager.MC);
        }
    }
}
