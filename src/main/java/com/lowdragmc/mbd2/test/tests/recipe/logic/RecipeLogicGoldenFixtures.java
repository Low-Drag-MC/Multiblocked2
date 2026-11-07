package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.BeforeRecipeWorkingEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.OnRecipeFinishEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.SetEventRecipeNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Fixtures for {@link RecipeLogicGoldenTests}: the recipe logic's observable behaviour, pinned before the
 * multi-logic rework. Ids and recipes here are part of the golden data — the frozen save in the tests names them —
 * so never edit them; add new ones instead.
 */
public class RecipeLogicGoldenFixtures implements TestFixtureProvider {
    public static final ResourceLocation RECIPE_TYPE = MBD2.id("golden_logic_recipes");
    public static final ResourceLocation FUEL_TYPE = MBD2.id("golden_fuel_recipes");
    public static final ResourceLocation FUELED_TYPE = MBD2.id("golden_fueled_recipes");

    /** 2 input slots (0, 1), 2 output slots (2, 3), 100k FE. */
    public static final ResourceLocation MACHINE = MBD2.id("golden_logic_machine");
    /** As {@link #MACHINE}, but inputs are taken when the recipe finishes. */
    public static final ResourceLocation AFTER_MACHINE = MBD2.id("golden_after_machine");
    /** 2 input slots (0, 1), 2 output slots (2, 3), runs {@link #FUELED_TYPE}, which burns {@link #FUEL_TYPE}. */
    public static final ResourceLocation FUEL_MACHINE = MBD2.id("golden_fuel_machine");
    /** "SCP" line: stone, controller, {@link #MB_PART}. The controller has no traits of its own. */
    public static final ResourceLocation MB_CONTROLLER = MBD2.id("golden_mb_controller");
    /** 1 input slot (0), 1 output slot (1). */
    public static final ResourceLocation MB_PART = MBD2.id("golden_mb_part");
    /** {@link #MACHINE} built as a {@link LegacyHookMachines.SimpleMachine}. */
    public static final ResourceLocation LEGACY_MACHINE = MBD2.id("golden_legacy_machine");
    /** {@link #MB_CONTROLLER} / {@link #MB_PART} built as {@link LegacyHookMachines.Controller} / {@link LegacyHookMachines.Part}. */
    public static final ResourceLocation LEGACY_CONTROLLER = MBD2.id("golden_legacy_controller");
    public static final ResourceLocation LEGACY_PART = MBD2.id("golden_legacy_part");
    /** {@link #MACHINE} with {@link #FROZEN_BLUEPRINT} bound — see {@link #markerGraph()}. */
    public static final ResourceLocation BLUEPRINT_MACHINE = MBD2.id("golden_blueprint_machine");
    /** The analog signal {@link #markerGraph()} sets when a recipe starts. */
    public static final int STARTED_MARKER = 3;
    /** The analog signal {@link #markerGraph()} sets when a recipe finishes. */
    public static final int FINISHED_MARKER = 7;

    /**
     * {@link #markerGraph()} as the build before the multi-logic rework saved it, when the recipe event nodes had no
     * {@code recipeLogic} port. Captured once, never regenerated: it is the old format, by definition.
     */
    public static final String FROZEN_BLUEPRINT = """
            {_additional:{nodes:[{_additional:{},_type:"custom",collapsed:0b,elementColor:-1,minWidth:0.0f,name:"",nodeClass:"com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode",
            position:[0.0f,0.0f],previewExpanded:1b,state:"_NULL_",title:{translate:"mbd2_event_recipe_modify_before"},uid:"cecd52ac-b673-4c7e-88dd-ec84b196e24d",
            userColor:0b},{_additional:{inputConstants:{in:{type:"EXECUTION_FLOW"},recipe:{type:"com.lowdragmc.mbd2.api.recipe.MBDRecipe"}}},
            _type:"custom",collapsed:0b,elementColor:-1,minWidth:0.0f,name:"",nodeClass:"com.lowdragmc.mbd2.common.blueprint.node.event.SetEventRecipeNode",
            position:[0.0f,0.0f],previewExpanded:1b,state:"_NULL_",title:{translate:"mbd2_event_set_recipe"},uid:"ec999d18-30b7-4785-a794-aae8aca0069d",
            userColor:0b},{_additional:{},_type:"custom",collapsed:0b,elementColor:-1,minWidth:0.0f,name:"",nodeClass:"com.lowdragmc.mbd2.common.blueprint.node.event.BeforeRecipeWorkingEventNode",
            position:[0.0f,0.0f],previewExpanded:1b,state:"_NULL_",title:{translate:"mbd2_event_before_recipe_working"},uid:"7e0dd8f9-4475-4d42-9c0a-49d1be370cb4",
            userColor:0b},{_additional:{inputConstants:{in:{type:"EXECUTION_FLOW"},machine:{type:"com.lowdragmc.mbd2.common.machine.MBDMachine"},
            signal:{defaultValue:0,type:"java.lang.Integer",value:3}}},_type:"custom",collapsed:0b,elementColor:-1,minWidth:0.0f,
            name:"",nodeClass:"com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes$SetAnalogSignal",position:[0.0f,
            0.0f],previewExpanded:1b,state:"_NULL_",title:{translate:"mbd2_redstone_set_analog_signal"},uid:"ce083671-1b04-4ed2-bf81-a0233a0b9140",
            userColor:0b},{_additional:{},_type:"custom",collapsed:0b,elementColor:-1,minWidth:0.0f,name:"",nodeClass:"com.lowdragmc.mbd2.common.blueprint.node.event.OnRecipeFinishEventNode",
            position:[0.0f,0.0f],previewExpanded:1b,state:"_NULL_",title:{translate:"mbd2_event_on_recipe_finish"},uid:"323e1a09-7048-4ec1-a56c-b69a8821522c",
            userColor:0b},{_additional:{inputConstants:{in:{type:"EXECUTION_FLOW"},machine:{type:"com.lowdragmc.mbd2.common.machine.MBDMachine"},
            signal:{defaultValue:0,type:"java.lang.Integer",value:7}}},_type:"custom",collapsed:0b,elementColor:-1,minWidth:0.0f,
            name:"",nodeClass:"com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes$SetAnalogSignal",position:[0.0f,
            0.0f],previewExpanded:1b,state:"_NULL_",title:{translate:"mbd2_redstone_set_analog_signal"},uid:"aefa473c-e45e-4bda-aecb-ac313759f4b9",
            userColor:0b}],placemats:[],portals:[],reroutePoints:[],sections:[{items:[],name:"",uid:"fa47e712-da9d-49a8-a4dd-860d32cba6b9"}],
            stickyNotes:[],variables:[],wires:[{_additional:{fromNodeUid:[I;-825404756,-1233957762,-1998721916,-1315511731],
            fromPortId:"next",fromPortUid:[I;-1535433501,1271673223,-1597157661,2114767272],toNodeUid:[I;-325477096,817317765,
            -1483429144,-1398798691],toPortId:"in",toPortUid:[I;710114354,2023895428,-1357203947,1672749157]},uid:"c00b0a76-4fa6-4d1a-9b6f-cf7702053501"},
            {_additional:{fromNodeUid:[I;-825404756,-1233957762,-1998721916,-1315511731],fromPortId:"recipe",fromPortUid:[I;252771850,
            727396476,-1523947972,-2097230500],toNodeUid:[I;-325477096,817317765,-1483429144,-1398798691],toPortId:"recipe",
            toPortUid:[I;882357259,-1660274344,-1332589854,-817082100]},uid:"bc1c0f28-ec43-46d7-a1a8-fc74c5d591d0"},{_additional:{fromNodeUid:[I;2114836729,
            1148538178,-1677047343,-1103688524],fromPortId:"next",fromPortUid:[I;-896445510,955202817,-1415672829,-52235593],
            toNodeUid:[I;-838322575,453267154,-1082023901,973836608],toPortId:"in",toPortUid:[I;-471038690,-801425633,-2112645814,
            -214884079]},uid:"182ec71e-a577-4c0e-82f6-0662c4a27dfc"},{_additional:{fromNodeUid:[I;842930697,1883786945,-1519602022,
            -2011082196],fromPortId:"next",fromPortUid:[I;-1200570674,585642063,-2075452941,772984964],toNodeUid:[I;-1359329476,
            -463582246,-1362383823,928642233],toPortId:"in",toPortUid:[I;-62217523,983121423,-1736574664,-1384724790]},uid:"8b28cb03-ec95-44b8-bdec-b35eb656bc7d"}]},
            uid:"653c1a11-e499-454e-9aaf-af825d57085e"}
            """;

    /** stone + 10 FE/t → dirt, 20 ticks. */
    public static final String PRESS = "golden_press";
    /** cobblestone → gravel, 10 ticks. */
    public static final String AFTER = "golden_after";
    /** andesite → diorite, 15 ticks. */
    public static final String MB = "golden_mb";
    /** sand → glass, 20 ticks, needs fuel. */
    public static final String SMELT = "golden_smelt";
    /** coal, burns 30 ticks. */
    public static final String COAL = "golden_coal";

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(RECIPE_TYPE)
                .recipe(PRESS, b -> b
                        .inputItems(Items.STONE)
                        .perTick(true).input(ForgeEnergyRecipeCapability.CAP, 10).perTick(false)
                        .outputItems(Items.DIRT)
                        .duration(20))
                .recipe(AFTER, b -> b
                        .inputItems(Items.COBBLESTONE)
                        .outputItems(Items.GRAVEL)
                        .duration(10))
                .recipe(MB, b -> b
                        .inputItems(Items.ANDESITE)
                        .outputItems(Items.DIORITE)
                        .duration(15))
                .register(event);
        TestRecipeTypeBuilder.of(FUEL_TYPE)
                .recipe(COAL, b -> b
                        .inputItems(Items.COAL)
                        .duration(30))
                .register(event);
        TestRecipeTypeBuilder.of(FUELED_TYPE)
                .configure(type -> {
                    type.getFuelRecipeConfig().setEnable(true);
                    type.getFuelRecipeConfig().getFuelRecipeTypes().add(FUEL_TYPE);
                })
                .recipe(SMELT, b -> b
                        .inputItems(Items.SAND)
                        .outputItems(Items.GLASS)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        logicMachine(MACHINE).register(event);
        logicMachine(AFTER_MACHINE).withConsumeInputsAfterWorking(true).register(event);
        TestMachineBuilder.simple(FUEL_MACHINE)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(FUELED_TYPE)
                .register(event);

        var part = mbPart(MB_PART).register(event);
        mbController(MB_CONTROLLER, part).register(event);

        logicMachine(LEGACY_MACHINE)
                .withMachineFactory(LegacyHookMachines.SimpleMachine::new)
                .register(event);
        var legacyPart = mbPart(LEGACY_PART)
                .withMachineFactory(LegacyHookMachines.Part::new)
                .register(event);
        mbController(LEGACY_CONTROLLER, legacyPart)
                .withMachineFactory(LegacyHookMachines.Controller::new)
                .register(event);
        logicMachine(BLUEPRINT_MACHINE)
                .withBlueprint(MachineBlueprintBinding.ofInline(frozenBlueprint()))
                .register(event);
    }

    /** The frozen graph, or the graph built from code while nothing is frozen yet. */
    public static CompoundTag frozenBlueprint() {
        if (FROZEN_BLUEPRINT.isBlank()) {
            return markerGraph().graphModel.serializeNBT(Platform.getFrozenRegistry());
        }
        try {
            return TagParser.parseTag(FROZEN_BLUEPRINT);
        } catch (Exception e) {
            throw new IllegalStateException("the frozen blueprint does not parse", e);
        }
    }

    /**
     * Three recipe events, each leading somewhere observable:
     * {@code Recipe Modify (Before) → Set Event Recipe} hands the recipe back unchanged — break that wire and the
     * recipe becomes null and never runs; {@code Before Recipe Working → Set Analog Signal(3)};
     * {@code On Recipe Finish → Set Analog Signal(7)}.
     */
    public static MachineBlueprintGraph markerGraph() {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var setRecipe = KGGameTestHelpers.addRegisteredNode(graph, SetEventRecipeNode.class);
        KGGameTestHelpers.wire(graph, setRecipe.getInputsById().get("in"), modify.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, setRecipe.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));

        var before = KGGameTestHelpers.addRegisteredNode(graph, BeforeRecipeWorkingEventNode.class);
        var started = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(started, "signal", STARTED_MARKER);
        KGGameTestHelpers.wire(graph, started.getInputsById().get("in"), before.getOutputsById().get("next"));

        var finish = KGGameTestHelpers.addRegisteredNode(graph, OnRecipeFinishEventNode.class);
        var finished = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(finished, "signal", FINISHED_MARKER);
        KGGameTestHelpers.wire(graph, finished.getInputsById().get("in"), finish.getOutputsById().get("next"));
        return graph;
    }

    private static TestMachineBuilder logicMachine(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withEnergy(100_000)
                .withRecipeType(RECIPE_TYPE);
    }

    private static TestMachineBuilder mbPart(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withRootState(MachineState.baseBuilder()
                        .child("formed", formed -> {})
                        .child("working", working -> {})
                        .child("waiting", waiting -> {})
                        .build())
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT);
    }

    private static TestMachineBuilder mbController(ResourceLocation id, MBDMachineDefinition part) {
        // FactoryBlockPattern.start() puts the leftmost char at world +X for the default NORTH facing, so with the
        // controller at (1, 1, 1) the part sits at (0, 1, 1) and the stone at (2, 1, 1).
        return TestMachineBuilder.multiblock(id)
                .withRecipeType(RECIPE_TYPE)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(part.block()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build());
    }
}
