package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.convert.ToIntNode;
import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.AddNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.ClampNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.DivideNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MaxNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MultiplyNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtGetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtSetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtValueType;
import com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.SetEventRecipeNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeBuildNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoNode;

/**
 * A machine that warms up as it runs and speeds up as it warms.
 *
 * <h2>What it does</h2>
 * Two behaviours sharing one number. Every tick, heat rises while the machine works and falls while it
 * idles, saved on the machine so it survives a chunk unload. Before every recipe, the duration is
 * divided by a factor that grows with heat, so a machine kept busy gets faster and one left alone
 * cools back down.
 *
 * <h2>Why this one is worth reading</h2>
 * It is the only built-in with <em>state</em>, and state is the thing a blueprint can do that a
 * machine's configuration cannot. The mechanism is small — read a number out of the machine's custom
 * data, change it, write it back — and it is the same mechanism for a maintenance counter, a cooldown,
 * a charge-up, or anything else that has to remember something between ticks.
 *
 * <h2>Two entry nodes, one graph</h2>
 * A blueprint is not "the graph for one event": it may hold as many entry nodes as it needs, and they
 * share the graph's variables. Splitting this into a heat blueprint and a speed blueprint would mean
 * two graphs that have to agree on an NBT key by convention, which is worse.
 *
 * <p>Custom data is synced to the client, so the heat is readable from a UI element without any extra
 * work — which is why it is stored there rather than in a runtime value.</p>
 *
 * <h2>Why it marks the recipe dirty</h2>
 * {@code IMachine.alwaysTryModifyRecipe()} is false by default, so a machine repeating one recipe
 * reuses the copy it already found and modified — meaning {@code Recipe Modify (Before)} fires once
 * and the duration computed on that first pass sticks forever. A blueprint whose modifier depends on
 * something that <em>changes</em> has to force a re-search, which is what the {@code Mark Recipe Dirty}
 * on the tick path does. It is gated behind {@code Every N Ticks} because a re-search is not free.
 *
 * <p>Nothing else here needs it: the overclock and part-count built-ins key off values that do not
 * move while a machine sits there. This is the one worth copying if a modifier ever looks like it
 * applied once and then stopped.</p>
 */
final class HeatBuildupBlueprint {

    private HeatBuildupBlueprint() {}

    /** The key the heat lives under in the machine's custom data. */
    private static final String KEY = "heat";

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                HEAT BUILDUP

                The machine warms up while it works and speeds up
                as it warms. It cools down while idle.

                heatPerTick    heat gained per working tick
                coolPerTick    heat lost per idle tick
                maxHeat        the cap
                bonusAtMaxHeat speed factor at full heat, so 2
                               means twice as fast when hot

                Heat is kept in the machine's custom data under
                'heat', so it survives the chunk unloading and can
                be shown in the machine's UI.

                Same mechanism for anything a machine has to
                remember: a maintenance counter, a cooldown, a
                charge-up.""");

        // ============ part one: the heat, every tick ============
        b.add("tick", TickEventNode.class, 0, 0)
                .add("info", MachineInfoNode.class, 0, 160)
                .block("info", "data", MachineInfoBlocks.CustomData.class)
                .add("logic", RecipeLogicInfoNode.class, 0, 360)
                .block("logic", "isWorking", RecipeLogicInfoBlocks.IsWorking.class)
                .add("heat", NbtGetNode.class, 230, 160)
                .option("heat", "valueType", NbtValueType.FLOAT)
                .constant("heat.key", KEY)
                .title("heat", "heat so far");

        b.wire("heat.tag", "data.value");

        b.group("What it remembers", 0, 0, 350, 430, BuiltinNotes.READ_GROUP);

        // Working heats, idle cools. One branch picking between two deltas rather than two exec paths,
        // so the write below happens exactly once either way.
        b.parameter("heatPerTick", float.class, 1f, 450, 300)
                .parameter("coolPerTick", float.class, 2f, 450, 420)
                .add("gate", BranchNode.class, 450, 0)
                .title("gate", "working?")
                .add("warmer", AddNode.class, 650, 130)
                .title("warmer", "heat + gain")
                .add("cooler", AddNode.class, 650, 430)
                .title("cooler", "heat - loss")
                .add("loss", MultiplyNode.class, 450, 540)
                .title("loss", "-coolPerTick")
                .constant("loss.in1", -1f);

        b.wire("gate.cond", "isWorking.value")
                .wire("warmer.in1", "heat.out")
                .wire("warmer.in2", "heatPerTick")
                .wire("loss.in2", "coolPerTick")
                .wire("cooler.in1", "heat.out")
                .wire("cooler.in2", "loss.out");
        b.then("tick", "gate");

        // Both branches converge on one write. Clamp is what keeps the number sane in both directions
        // — an idle machine would otherwise cool forever into negative heat.
        b.parameter("maxHeat", float.class, 1000f, 880, 430)
                .add("clampWarm", ClampNode.class, 880, 130)
                .constant("clampWarm.min", 0f)
                .title("clampWarm", "0..maxHeat")
                .add("clampCool", ClampNode.class, 880, 300)
                .constant("clampCool.min", 0f)
                .title("clampCool", "0..maxHeat")
                .add("writeWarm", NbtSetNode.class, 1120, 0)
                .option("writeWarm", "valueType", NbtValueType.FLOAT)
                .constant("writeWarm.key", KEY)
                .title("writeWarm", "remember it")
                .add("writeCool", NbtSetNode.class, 1120, 300)
                .option("writeCool", "valueType", NbtValueType.FLOAT)
                .constant("writeCool.key", KEY)
                .title("writeCool", "remember it")
                .add("saveWarm", MachineActionNodes.MergeCustomData.class, 1340, 0)
                .title("saveWarm", "save")
                .add("saveCool", MachineActionNodes.MergeCustomData.class, 1340, 300)
                .title("saveCool", "save");

        b.wire("clampWarm.in", "warmer.out")
                .wire("clampWarm.max", "maxHeat")
                .wire("clampCool.in", "cooler.out")
                .wire("clampCool.max", "maxHeat")
                .wire("writeWarm.tag", "data.value")
                .wire("writeWarm.value", "clampWarm.out")
                .wire("writeCool.tag", "data.value")
                .wire("writeCool.value", "clampCool.out")
                .wire("saveWarm.data", "writeWarm.out")
                .wire("saveCool.data", "writeCool.out");

        b.wire("saveWarm.in", "gate.trueExec");
        b.wire("saveCool.in", "gate.falseExec");

        // Without this the speed half below would only ever apply once. A machine reuses the recipe it
        // already found and modified (IMachine.alwaysTryModifyRecipe is false by default), so the
        // duration computed on the first pass — while cold — would stick for every repeat. Marking the
        // recipe dirty forces a fresh search, and with it a fresh Recipe Modify. Every N Ticks keeps
        // that to once a second rather than once a tick, because a re-search is not free.
        b.add("every", MachineInfoNode.class, 1560, 0)
                .block("every", "second", MachineInfoBlocks.EveryNTicks.class)
                .constant("second.interval", 20)
                .add("recheck", BranchNode.class, 1740, 0)
                .title("recheck", "time to re-price?")
                .add("refresh", RecipeLogicActionNodes.MarkRecipeDirty.class, 1920, 0)
                .title("refresh", "re-apply the speed");

        b.wire("recheck.cond", "second.value");
        b.then("saveWarm", "recheck");
        b.wire("refresh.in", "recheck.trueExec");

        b.note(450, 660, 520, """
                Merge Custom Data rather than Set: another blueprint
                may be keeping its own key in the same tag, and Set
                would drop it. Merge only touches 'heat'.""");

        b.group("Warm up, or cool down", 450, 0, 1660, 600, BuiltinNotes.DECIDE_GROUP);

        // ============ part two: the speed, before each recipe ============
        b.add("modify", RecipeModifyBeforeEventNode.class, 0, 860)
                .add("recipe", RecipeInfoNode.class, 0, 1010)
                .block("recipe", "duration", RecipeInfoBlocks.Duration.class)
                .add("info2", MachineInfoNode.class, 0, 1200)
                .block("info2", "data2", MachineInfoBlocks.CustomData.class)
                .add("heat2", NbtGetNode.class, 230, 1200)
                .option("heat2", "valueType", NbtValueType.FLOAT)
                .constant("heat2.key", KEY)
                .title("heat2", "heat now");

        b.wire("recipe.target", "modify.recipe")
                .wire("heat2.tag", "data2.value");

        // heat/maxHeat scaled into 1..bonus — the same 0..1-then-remap shape the comparator uses.
        b.read("maxHeat2", "maxHeat", 450, 1330)
                .add("ratio", DivideNode.class, 450, 1200)
                .title("ratio", "heat / maxHeat")
                .parameter("bonusAtMaxHeat", float.class, 2f, 650, 1330)
                .add("extra", MultiplyNode.class, 650, 1200)
                .title("extra", "how much bonus")
                .add("factor", AddNode.class, 850, 1200)
                .title("factor", "speed factor")
                .constant("factor.in1", 1f)
                .add("gain", AddNode.class, 650, 1450)
                .title("gain", "bonus - 1")
                .constant("gain.in2", -1f);

        b.wire("ratio.a", "heat2.out")
                .wire("ratio.b", "maxHeat2")
                .wire("gain.in1", "bonusAtMaxHeat")
                .wire("extra.in1", "ratio.out")
                .wire("extra.in2", "gain.out")
                .wire("factor.in2", "extra.out");

        b.add("newDuration", DivideNode.class, 1050, 1010)
                .title("newDuration", "duration / factor")
                .add("atLeastOne", MaxNode.class, 1230, 1010)
                .constant("atLeastOne.in2", 1f)
                .title("atLeastOne", "never below 1 tick")
                .add("ticks", ToIntNode.class, 1410, 1010)
                .option("ticks", "op", ToIntNode.Op.ROUND)
                .title("ticks", "whole ticks");

        b.wire("newDuration.a", "duration.value")
                .wire("newDuration.b", "factor.out")
                .wire("atLeastOne.in1", "newDuration.out")
                .wire("ticks.in", "atLeastOne.out");

        b.add("faster", RecipeBuildNodes.SetDuration.class, 1610, 860)
                .title("faster", "duration = ticks")
                .add("apply", SetEventRecipeNode.class, 1790, 860)
                .title("apply", "use this recipe");

        b.wire("faster.recipe", "modify.recipe")
                .wire("faster.duration", "ticks.out")
                .wire("apply.recipe", "faster.result");
        b.then("modify", "apply");

        b.group("Speed, from the heat", 0, 860, 1930, 700, BuiltinNotes.ACT_GROUP);

        b.note(1050, 1330, 500, """
                At heat 0 the factor is 1, so a cold machine runs
                the recipe exactly as authored. That is worth
                keeping in any blueprint that scales something:
                the neutral value should be the untouched one.""");

        return b;
    }
}
