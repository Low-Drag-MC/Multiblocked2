package com.lowdragmc.mbd2.test.uitest.wiki;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.gui.MBDBindingIDs;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.List;

/** The machines the wiki videos show. Dev only, like every fixture. */
public class WikiDemoFixtures implements TestFixtureProvider {
    /** Stone → dirt in 3 s. */
    public static final ResourceLocation PRESS_TYPE = MBD2.id("wiki_press_recipes");
    /** Cobblestone → gravel in 4 s. */
    public static final ResourceLocation CRUSH_TYPE = MBD2.id("wiki_crush_recipes");
    /** Logic {@code press} (primary) and {@code crush}, each with an input and an output slot of its own. */
    public static final ResourceLocation TWO_LOGICS = MBD2.id("wiki_two_logics");
    /** Logic {@code main} with three threads over one input and one output slot. */
    public static final ResourceLocation THREADS = MBD2.id("wiki_threads");
    /** Two item slots holding 4096 of anything, kept in the machine item when it is broken. */
    public static final ResourceLocation LARGE_SLOTS = MBD2.id("wiki_large_slots");

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(PRESS_TYPE)
                .recipe("wiki_press", b -> b.inputItems(Items.STONE).outputItems(Items.DIRT).duration(60))
                .register(event);
        TestRecipeTypeBuilder.of(CRUSH_TYPE)
                .recipe("wiki_crush", b -> b.inputItems(Items.COBBLESTONE).outputItems(Items.GRAVEL).duration(80))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(TWO_LOGICS)
                .withPrimaryRecipeLogic("press", 1)
                .withRecipeType(PRESS_TYPE)
                .withAdditionalRecipeLogic("crush", CRUSH_TYPE, 1)
                .withItemSlots(1, IO.IN, def -> scoped(def, "press_in", "press"))
                .withItemSlots(1, IO.OUT, def -> scoped(def, "press_out", "press"))
                .withItemSlots(1, IO.IN, def -> scoped(def, "crush_in", "crush"))
                .withItemSlots(1, IO.OUT, def -> scoped(def, "crush_out", "crush"))
                .withUI(ui("Two recipe logics", row("press", "press_in", MBDBindingIDs.PROGRESS_BAR, "press_out"),
                        row("crush", "crush_in", MBDBindingIDs.forRecipeLogic(MBDBindingIDs.PROGRESS_BAR, "crush"), "crush_out")))
                .register(event);
        TestMachineBuilder.simple(THREADS)
                .withPrimaryRecipeLogic("main", 3)
                .withRecipeType(PRESS_TYPE)
                .withItemSlots(1, IO.IN, def -> def.setName("in"))
                .withItemSlots(1, IO.OUT, def -> def.setName("out"))
                .withUI(ui("Three threads",
                        row("main", "in", MBDBindingIDs.PROGRESS_BAR, "out"),
                        row("main#1", null, MBDBindingIDs.forRecipeLogic(MBDBindingIDs.PROGRESS_BAR, "main#1"), null),
                        row("main#2", null, MBDBindingIDs.forRecipeLogic(MBDBindingIDs.PROGRESS_BAR, "main#2"), null)))
                .register(event);
        TestMachineBuilder.simple(LARGE_SLOTS)
                .withItemSlots(2, IO.BOTH, def -> {
                    def.setName("storage");
                    def.setSlotLimit(4096);
                    def.setIgnoreItemStackLimit(true);
                    def.setKeepDataOnDrop(true);
                })
                .withUI(ui("4096 per slot", slots("storage", 2)))
                .register(event);
    }

    private static void scoped(ItemSlotCapabilityTraitDefinition def, String name, String logic) {
        def.setName(name);
        def.setRecipeLogics(List.of(logic));
    }

    private static UITemplate ui(String title, UIElement... rows) {
        var root = new UIElement();
        root.setId("wiki_machine");
        root.addChild(new Label().setText(title));
        for (var row : rows) root.addChild(row);
        root.addChild(new UIElement().layout(l -> l.height(6)));
        root.addChild(new InventorySlots());
        root.addClass("panel_bg");
        return UITemplate.of(root, StylesheetManager.MC_MERGED);
    }

    /** {@code label  [in] [bar] [out]}; a null slot leaves its place empty. */
    private static UIElement row(String label, String in, String bar, String out) {
        var row = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(4).height(20));
        row.addChild(new Label().setText(label).layout(l -> l.width(44)));
        row.addChild(slotOrGap(in));
        var progress = new ProgressBar();
        progress.setId(bar);
        progress.layout(l -> l.width(70).height(14));
        row.addChild(progress);
        row.addChild(slotOrGap(out));
        return row;
    }

    private static UIElement slotOrGap(String trait) {
        if (trait == null) return new UIElement().layout(l -> l.width(18).height(18));
        var slot = new ItemSlot();
        slot.setId("ui:" + trait + "_0");
        return slot;
    }

    private static UIElement slots(String trait, int count) {
        var row = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(4).height(20));
        for (int i = 0; i < count; i++) {
            var slot = new ItemSlot();
            slot.setId("ui:" + trait + "_" + i);
            row.addChild(slot);
        }
        return row;
    }
}
