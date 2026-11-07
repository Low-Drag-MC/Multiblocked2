package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.builtin.BlueprintBuilder;
import com.lowdragmc.mbd2.common.blueprint.node.event.OnRecipeWorkingEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoNode;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.gui.MBDBindingIDs;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineRecipeStatusChangedEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.function.Consumer;

/**
 * Machines with several recipe logics, for {@link MultiRecipeLogicTests}. Two recipe types that never compete for
 * an input, so which logic ran what shows in the outputs.
 */
public class MultiRecipeLogicFixtures implements TestFixtureProvider {
    /** stone + 10 FE/t → dirt, 20 ticks. */
    public static final ResourceLocation TYPE_A = MBD2.id("multi_logic_a_recipes");
    /** cobblestone + 10 FE/t → gravel, 30 ticks. */
    public static final ResourceLocation TYPE_B = MBD2.id("multi_logic_b_recipes");

    /**
     * Logic {@code a} (primary, runs {@link #TYPE_A}) and {@code b} (runs {@link #TYPE_B}), each with its own scoped
     * input and output slot — a 0, a's out 1, b 2, b's out 3 — and one shared 100k FE store.
     */
    public static final ResourceLocation ISOLATED = MBD2.id("multi_logic_isolated");
    /** {@link #ISOLATED}, with {@code b} taking its inputs when it finishes. */
    public static final ResourceLocation ISOLATED_AFTER = MBD2.id("multi_logic_isolated_after");
    /** {@link #ISOLATED} with a UI of two progress bars: {@code ui:progress_bar} for a, {@code ui:progress_bar@b} for b. */
    public static final ResourceLocation ISOLATED_UI = MBD2.id("multi_logic_isolated_ui");
    /** {@link #ISOLATED} built as a {@link LegacyHookMachines.SimpleMachine}. */
    public static final ResourceLocation ISOLATED_LEGACY = MBD2.id("multi_logic_isolated_legacy");
    /** {@link #ISOLATED} built as a {@link NewHookMachines.SimpleMachine}. */
    public static final ResourceLocation ISOLATED_NEW = MBD2.id("multi_logic_isolated_new");
    /** {@link #ISOLATED} with a blueprint suspending, with the logic input unwired, whichever logic works. */
    public static final ResourceLocation ISOLATED_SUSPENDER = MBD2.id("multi_logic_isolated_suspender");
    public static final int SUSPENDED_MARKER = 6;
    /** {@link #ISOLATED} whose {@code b} pauses while the primary works, see {@link #listenForPauser()}. */
    public static final ResourceLocation ISOLATED_PAUSER = MBD2.id("multi_logic_isolated_pauser");
    private static boolean pauserListening;
    /** {@link #ISOLATED} with two threads of {@code a}, switched off every tick by an unwired Set Working Enabled. */
    public static final ResourceLocation ISOLATED_SWITCHED = MBD2.id("multi_logic_isolated_switched");
    /** {@link #ISOLATED} whose Set Working Enabled(false) every tick is wired from Recipe Logic At, address unset. */
    public static final ResourceLocation ISOLATED_PRIMARY_OFF = MBD2.id("multi_logic_isolated_primary_off");
    /** {@link #ISOLATED} with "Has Recipe Logic" switched off, and two threads of {@code a} asked for. */
    public static final ResourceLocation SWITCHED_OFF = MBD2.id("multi_logic_switched_off");
    /** Three threads of the primary {@code main} logic over 3 shared input slots (0-2), 1 output slot (3), 100k FE. */
    public static final ResourceLocation THREADS = MBD2.id("multi_logic_threads");
    /** {@link #THREADS} with a blueprint putting the thread index of {@code main#2}, looked up by address, on its signal. */
    public static final ResourceLocation THREADS_INDEXER = MBD2.id("multi_logic_threads_indexer");
    /** Two threads of {@code main}, each with a slot of its own: in 0 for #0, in 1 for #1, out 2 for #0, out 3 for #1. */
    public static final ResourceLocation FACTORY = MBD2.id("multi_logic_factory");
    /**
     * A {@link LegacyHookMachines.LegacyGettersMachine}: its definition runs {@link #TYPE_A}, its class
     * {@link #TYPE_B}. An input slot (0), an output slot (1), 100k FE.
     */
    public static final ResourceLocation LEGACY_GETTERS = MBD2.id("multi_logic_legacy_getters");
    /**
     * "XCY": logics {@code a} and {@code b} on a controller with 100k FE of its own; the part at X (2,1,1) is
     * matched for {@code a}, the part at Y (0,1,1) for {@code b}.
     */
    public static final ResourceLocation MB_CONTROLLER = MBD2.id("multi_logic_mb_controller");
    /** "XCY" like {@link #MB_CONTROLLER}, but X is matched for {@code b} with IO NONE: no logic gets its handlers. */
    public static final ResourceLocation MB_CONTROLLER_SENSOR = MBD2.id("multi_logic_mb_controller_sensor");
    /** One input slot (0), one output slot (1). */
    public static final ResourceLocation MB_PART = MBD2.id("multi_logic_mb_part");
    /** Like {@link #MB_PART}, but its slots are scoped to {@code a}: matched at Y (for {@code b}) nobody may use them. */
    public static final ResourceLocation MB_PART_FOR_A = MBD2.id("multi_logic_mb_part_for_a");
    /** {@link #MB_PART} as a {@link NewHookMachines.Part}, writing down the status changes it is told. */
    public static final ResourceLocation MB_STATUS_PART = MBD2.id("multi_logic_mb_status_part");

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(TYPE_A)
                .recipe("multi_press", b -> b
                        .inputItems(Items.STONE)
                        .perTick(true).input(ForgeEnergyRecipeCapability.CAP, 10).perTick(false)
                        .outputItems(Items.DIRT)
                        .duration(20))
                .register(event);
        TestRecipeTypeBuilder.of(TYPE_B)
                .recipe("multi_crush", b -> b
                        .inputItems(Items.COBBLESTONE)
                        .perTick(true).input(ForgeEnergyRecipeCapability.CAP, 10).perTick(false)
                        .outputItems(Items.GRAVEL)
                        .duration(30))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        isolated(ISOLATED).register(event);
        isolatedWith(ISOLATED_AFTER, b -> b.consumeInputsAfterWorking(true)).register(event);
        isolated(ISOLATED_UI).withUI(twoProgressBars()).register(event);
        isolated(ISOLATED_LEGACY).withMachineFactory(LegacyHookMachines.SimpleMachine::new).register(event);
        isolated(ISOLATED_NEW).withMachineFactory(NewHookMachines.SimpleMachine::new).register(event);
        isolated(ISOLATED_SUSPENDER).withBlueprint(suspendWhoeverWorks()).register(event);
        isolated(ISOLATED_PAUSER).register(event);
        isolated(SWITCHED_OFF).withPrimaryRecipeLogic("a", 2).withoutRecipeLogic().register(event);
        isolated(ISOLATED_SWITCHED).withPrimaryRecipeLogic("a", 2).withBlueprint(switchOffEveryTick(false)).register(event);
        isolated(ISOLATED_PRIMARY_OFF).withBlueprint(switchOffEveryTick(true)).register(event);

        TestMachineBuilder.simple(THREADS)
                .withPrimaryRecipeLogic("main", 3)
                .withRecipeType(TYPE_A)
                .withItemSlots(3, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withEnergy(100_000)
                .register(event);
        TestMachineBuilder.simple(THREADS_INDEXER)
                .withPrimaryRecipeLogic("main", 3)
                .withRecipeType(TYPE_A)
                .withBlueprint(indexOfThirdThread())
                .register(event);
        TestMachineBuilder.simple(FACTORY)
                .withPrimaryRecipeLogic("main", 2)
                .withRecipeType(TYPE_A)
                .withItemSlots(1, IO.IN, def -> def.setRecipeLogics(List.of("main#0")))
                .withItemSlots(1, IO.IN, def -> def.setRecipeLogics(List.of("main#1")))
                .withItemSlots(1, IO.OUT, def -> def.setRecipeLogics(List.of("main#0")))
                .withItemSlots(1, IO.OUT, def -> def.setRecipeLogics(List.of("main#1")))
                .withEnergy(100_000)
                .register(event);
        TestMachineBuilder.simple(LEGACY_GETTERS)
                .withRecipeType(TYPE_A)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withEnergy(100_000)
                .withMachineFactory(LegacyHookMachines.LegacyGettersMachine::new)
                .register(event);

        var part = mbPart(MB_PART).register(event);
        var partForA = mbPart(MB_PART_FOR_A)
                .withItemSlots(1, IO.IN, def -> {
                    def.setName("scoped_in");
                    def.setRecipeLogics(List.of("a"));
                })
                .register(event);
        var statusPart = mbPart(MB_STATUS_PART).withMachineFactory(NewHookMachines.Part::new).register(event);
        TestMachineBuilder.multiblock(MB_CONTROLLER)
                .withPrimaryRecipeLogic("a", 1)
                .withRecipeType(TYPE_A)
                .withAdditionalRecipeLogic("b", TYPE_B, 1)
                .withEnergy(100_000)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("XCY")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.blocks(part.block(), partForA.block(), statusPart.block()).setRecipeLogics("a"))
                        .where('Y', Predicates.blocks(part.block(), partForA.block(), statusPart.block()).setRecipeLogics("b"))
                        .build())
                .register(event);
        TestMachineBuilder.multiblock(MB_CONTROLLER_SENSOR)
                .withPrimaryRecipeLogic("a", 1)
                .withRecipeType(TYPE_A)
                .withAdditionalRecipeLogic("b", TYPE_B, 1)
                .withEnergy(100_000)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("XCY")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.blocks(part.block()).setRecipeLogics("b").setIO(IO.NONE))
                        .where('Y', Predicates.blocks(part.block()))
                        .build())
                .register(event);
    }

    /**
     * Pause {@code b} of an {@link #ISOLATED_PAUSER} while its primary works, and resume it otherwise: from a
     * status-changed listener, as the wiki's KubeJS example does. Listens once, for that machine only.
     */
    public static synchronized void listenForPauser() {
        if (pauserListening) return;
        pauserListening = true;
        NeoForge.EVENT_BUS.addListener(MachineRecipeStatusChangedEvent.class, event -> {
            var machine = event.getMachine();
            if (!machine.getDefinition().id().equals(ISOLATED_PAUSER) || !event.recipeLogic.isPrimary()) return;
            var b = machine.getRecipeLogic("b");
            if (b != null) {
                b.setWorkingEnabled(event.newStatus != RecipeLogic.Status.WORKING);
            }
        });
    }

    private static TestMachineBuilder isolated(ResourceLocation id) {
        return isolatedWith(id, b -> {});
    }

    private static TestMachineBuilder isolatedWith(ResourceLocation id,
                                                   Consumer<ConfigRecipeLogicSettings.ConfigRecipeLogicSettingsBuilder> tweakB) {
        return TestMachineBuilder.simple(id)
                .withPrimaryRecipeLogic("a", 1)
                .withRecipeType(TYPE_A)
                .withAdditionalRecipeLogic("b", TYPE_B, 1, tweakB)
                .withItemSlots(1, IO.IN, def -> def.setRecipeLogics(List.of("a")))
                .withItemSlots(1, IO.OUT, def -> def.setRecipeLogics(List.of("a")))
                .withItemSlots(1, IO.IN, def -> def.setRecipeLogics(List.of("b")))
                .withItemSlots(1, IO.OUT, def -> def.setRecipeLogics(List.of("b")))
                .withEnergy(100_000);
    }

    private static TestMachineBuilder mbPart(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withRootState(MachineState.baseBuilder()
                        .child("formed", formed -> {})
                        .child("working", working -> {})
                        .build())
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT);
    }

    private static UITemplate twoProgressBars() {
        var root = new UIElement();
        root.setId("two_bars");
        root.layout(layout -> layout.width(120).height(60).paddingAll(4).gapAll(4));
        for (var id : new String[]{MBDBindingIDs.PROGRESS_BAR, MBDBindingIDs.forRecipeLogic(MBDBindingIDs.PROGRESS_BAR, "b")}) {
            var bar = new ProgressBar();
            bar.setId(id);
            bar.layout(layout -> layout.width(100).height(14));
            root.addChild(bar);
        }
        return UITemplate.of(root);
    }

    /** {@code Tick → Set Analog Signal}, the signal being Recipe Logic At("main#2") → Thread Index. */
    private static MachineBlueprintGraph indexOfThirdThread() {
        var b = BlueprintBuilder.create();
        b.add("tick", TickEventNode.class, 0, 0)
                .add("machine", MachineInfoNode.class, 0, 100)
                .block("machine", "at", MachineInfoBlocks.MachineRecipeLogicAt.class)
                .constant("at.address", "main#2")
                .add("logic", RecipeLogicInfoNode.class, 200, 100)
                .block("logic", "index", RecipeLogicInfoBlocks.ThreadIndex.class)
                .add("signal", MachineRedstoneNodes.SetAnalogSignal.class, 400, 0);
        b.wire("logic.target", "at.value");
        b.wire("signal.signal", "index.value");
        b.wire("signal.in", "tick.next");
        return b.getGraph();
    }

    /** {@code Tick → Set Working Enabled(false)}, its logic unwired, or wired from Recipe Logic At left at its default. */
    private static MachineBlueprintGraph switchOffEveryTick(boolean wiredToDefaultAddress) {
        var b = BlueprintBuilder.create();
        b.add("tick", TickEventNode.class, 0, 0)
                .add("off", RecipeLogicActionNodes.SetWorkingEnabled.class, 200, 0)
                .constant("off.enabled", false);
        b.wire("off.in", "tick.next");
        if (wiredToDefaultAddress) {
            b.add("machine", MachineInfoNode.class, 0, 100)
                    .block("machine", "at", MachineInfoBlocks.MachineRecipeLogicAt.class);
            b.wire("off.recipeLogic", "at.value");
        }
        return b.getGraph();
    }

    /** {@code On Recipe Working → Set Working Enabled(false) → Set Analog Signal(6)}, the logic input left unwired. */
    private static MachineBlueprintGraph suspendWhoeverWorks() {
        var graph = new MachineBlueprintGraph();
        var working = KGGameTestHelpers.addRegisteredNode(graph, OnRecipeWorkingEventNode.class);
        var disable = KGGameTestHelpers.addRegisteredNode(graph, RecipeLogicActionNodes.SetWorkingEnabled.class);
        KGGameTestHelpers.setInputConstant(disable, "enabled", false);
        var marker = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(marker, "signal", SUSPENDED_MARKER);
        KGGameTestHelpers.wire(graph, disable.getInputsById().get("in"), working.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, marker.getInputsById().get("in"), disable.getOutputsById().get("next"));
        return graph;
    }
}
